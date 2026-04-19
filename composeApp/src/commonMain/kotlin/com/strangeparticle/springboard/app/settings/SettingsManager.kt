package com.strangeparticle.springboard.app.settings

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

/**
 * The central settings manager. Resolves effective values using the registry
 * and a fixed immutable precedence chain, provides typed getters, and
 * tracks which source is responsible for the current resolved value.
 *
 * Each config source is backed by a typed [SettingsValues] instance.
 * Resolution walks the precedence chain from highest to lowest; the first
 * source that provides a non-null value for a given key wins.
 *
 * TODO: If a future nullable setting needs to distinguish "not provided"
 * from "explicitly set to null", the source-layer model will need an
 * additional presence-tracking mechanism.
 */
class SettingsManager(
    val runtimeEnvironment: RuntimeEnvironment,
    private val persistenceService: PersistenceService,
) {

    /**
     * Per-source value layers. Each config source has its own [SettingsValues]
     * instance. All layers start empty and are populated by [loadSettingsAtStartup].
     */
    private val layers: MutableMap<SettingsSource, SettingsValues> = mutableMapOf<SettingsSource, SettingsValues>().apply {
        for (source in PRECEDENCE_CHAIN) {
            put(source, SettingsValues())
        }
    }

    /**
     * Populates all settings layers at startup.
     *
     * 1. Init defaults from the registry's hardcoded values.
     * 2. Load persisted user settings.
     * 3. Load environment variables.
     * 4. Load CLI flags (desktop) or URL params (WASM).
     */
    fun loadSettingsAtStartup(
        environmentVariables: Map<String, String> = emptyMap(),
        commandLineArgs: List<String> = emptyList(),
        urlParams: Map<String, String> = emptyMap(),
    ) {
        initDefaultSettings()
        loadPersistedSettings()
        loadEnvironmentVariables(environmentVariables)
        loadCommandLineArgs(commandLineArgs)
        loadUrlParams(urlParams)
    }

    // -- Typed Getters --

    fun getBoolean(key: SettingsKey): Boolean {
        val entry = SettingsRegistry.require(key)
        require(entry.type == Boolean::class) {
            "Setting $key is not a Boolean (type: ${entry.type.simpleName})"
        }
        return resolveValue(key) as Boolean
    }

    fun getString(key: SettingsKey): String {
        val entry = SettingsRegistry.require(key)
        require(entry.type == String::class) {
            "Setting $key is not a String (type: ${entry.type.simpleName})"
        }
        return resolveValue(key) as String
    }

    fun getFilePath(key: SettingsKey): FilePath? {
        val entry = SettingsRegistry.require(key)
        require(entry.type == FilePath::class) {
            "Setting $key is not a FilePath (type: ${entry.type.simpleName})"
        }
        return resolveValue(key) as FilePath?
    }

    @Suppress("UNCHECKED_CAST") // List<String> has erased generics; the cast to List<String> can't be checked at runtime
    fun getStringList(key: SettingsKey): List<String> {
        val entry = SettingsRegistry.require(key)
        require(entry.type == List::class) {
            "Setting $key is not a List (type: ${entry.type.simpleName})"
        }
        return (resolveValue(key) as? List<String>) ?: emptyList()
    }

    fun getSelectedOptionIdFromDropDown(key: SettingsKey): String {
        val entry = SettingsRegistry.require(key)
        require(entry.type == StringFromDropDown::class) {
            "Setting $key is not available in the settings registry and/or is not of type StringFromDropDown (actual type: ${entry.type.simpleName})"
        }
        return resolveValue(key) as String
    }

    // -- Source Tracking --

    /**
     * Returns the [SettingsSource] responsible for the current resolved value of the given key.
     */
    fun getEffectiveSource(key: SettingsKey): SettingsSource {
        for (source in PRECEDENCE_CHAIN) {
            val layer = layers[source] ?: continue
            if (layer.isSet(key)) {
                return source
            }
        }
        return SettingsSource.APP_DEFAULT
    }

    fun getSource(key: SettingsKey): SettingsSource = getEffectiveSource(key)

    fun getValueFromSource(key: SettingsKey, source: SettingsSource): Any? {
        val layer = layers[source] ?: return null
        return if (layer.isSet(key)) layer.get(key) else null
    }

    /**
     * Returns true if the user has explicitly set this setting
     * (effective source is either session or persisted user settings).
     */
    fun isOverridden(key: SettingsKey): Boolean {
        val source = getEffectiveSource(key)
        return source == SettingsSource.USER_SETTINGS_FROM_SESSION || source == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE
    }

    /**
     * Returns the resolved value for the given key, applying the precedence chain.
     * Walks highest-to-lowest priority; the first source that provides a non-null value wins.
     */
    fun resolveValue(key: SettingsKey): Any? {
        for (source in PRECEDENCE_CHAIN) {
            val layer = layers[source] ?: continue
            if (layer.isSet(key)) {
                return layer.get(key)
            }
        }
        return SettingsRegistry.require(key).defaultValue
    }

    /**
     * Returns all settings applicable to the current runtime environment.
     */
    fun applicableSettings(): List<SettingItem> =
        SettingsRegistry.settingsForEnvironment(runtimeEnvironment)

    // -- User Settings Mutation --

    /**
     * Updates a user setting value. Writes to both the in-memory session layer
     * (for immediate effect) and the persistence layer (for next launch).
     */
    fun setUserSetting(key: SettingsKey, value: Any?) {
        val entry = SettingsRegistry.require(key)
        if (entry.type == StringFromDropDown::class && value is String) {
            val declaration = entry.defaultValue as StringFromDropDown
            require(declaration.isAllowed(value)) {
                "'$value' is not an allowed value for $key"
            }
        }
        layers[SettingsSource.USER_SETTINGS_FROM_SESSION] = layers.getValue(SettingsSource.USER_SETTINGS_FROM_SESSION).withSetting(key, value)
        layers[SettingsSource.USER_SETTINGS_FROM_PERSISTENCE] = layers.getValue(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE).withSetting(key, value)
        persistSettingsLayer()
    }

    // -- Private Loading --

    private fun initDefaultSettings() {
        var defaults = SettingsValues()
        for (item in SettingsRegistry.settingsForEnvironment(runtimeEnvironment)) {
            val storedDefault: Any? = if (item.type == StringFromDropDown::class) {
                (item.defaultValue as StringFromDropDown).defaultDropDownOptionId
            } else {
                item.defaultValue
            }
            defaults = defaults.withSetting(item.key, storedDefault)
        }
        layers[SettingsSource.APP_DEFAULT] = defaults
    }

    private fun loadPersistedSettings() {
        val dto = persistenceService.loadSettings()
        if (dto != null) {
            layers[SettingsSource.USER_SETTINGS_FROM_PERSISTENCE] = dto.toSettingsValues()
        }
    }

    private fun loadEnvironmentVariables(envVars: Map<String, String>) {
        val applicableKeys = SettingsRegistry.settingsForEnvironment(runtimeEnvironment).map { it.key }.toSet()
        var envValues = SettingsValues()

        for ((envVarName, rawValue) in envVars) {
            val entry = SettingsRegistry.findByEnvVarName(envVarName)
            if (entry == null || entry.key !in applicableKeys) continue

            try {
                val typedValue = coerceStringValue(entry, rawValue)
                envValues = envValues.withSetting(entry.key, typedValue)
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid environment variable '$envVarName': ${e.message}")
            }
        }

        layers[SettingsSource.ENVIRONMENT_VARIABLE] = envValues
    }

    private fun loadCommandLineArgs(args: List<String>) {
        val applicableKeys = SettingsRegistry.settingsForEnvironment(runtimeEnvironment).map { it.key }.toSet()
        var cliValues = SettingsValues()

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            if (!arg.startsWith("--")) {
                i++
                continue
            }

            val entry = SettingsRegistry.findByUrlParamName(arg.removePrefix("--"))
            if (entry == null || entry.key !in applicableKeys) {
                i++
                continue
            }

            try {
                when (entry.type) {
                    Boolean::class -> {
                        cliValues = cliValues.withSetting(entry.key, true)
                    }
                    List::class -> {
                        val nextArg = args.getOrNull(i + 1)
                        if (nextArg == null || nextArg.startsWith("--")) {
                            ToastBroadcaster.warning("CLI parameter '$arg' requires a value")
                        } else {
                            val items = nextArg.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            cliValues = cliValues.withSetting(entry.key, items)
                            i++
                        }
                    }
                    String::class, FilePath::class, StringFromDropDown::class -> {
                        val nextArg = args.getOrNull(i + 1)
                        if (nextArg == null || nextArg.startsWith("--")) {
                            ToastBroadcaster.warning("CLI parameter '$arg' requires a value")
                        } else {
                            val typedValue = coerceStringValue(entry, nextArg)
                            cliValues = cliValues.withSetting(entry.key, typedValue)
                            i++
                        }
                    }
                }
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid CLI parameter '$arg': ${e.message}")
            }
            i++
        }

        layers[SettingsSource.CLI_FLAG] = cliValues
    }

    private fun loadUrlParams(urlParams: Map<String, String>) {
        val applicableKeys = SettingsRegistry.settingsForEnvironment(runtimeEnvironment).map { it.key }.toSet()
        var urlValues = SettingsValues()

        for ((paramName, rawValue) in urlParams) {
            val entry = SettingsRegistry.findByUrlParamName(paramName)
            if (entry == null || entry.key !in applicableKeys) continue

            try {
                val typedValue = coerceStringValue(entry, rawValue)
                urlValues = urlValues.withSetting(entry.key, typedValue)
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid URL parameter '$paramName': ${e.message}")
            }
        }

        layers[SettingsSource.URL_PARAM] = urlValues
    }

    private fun persistSettingsLayer() {
        val settingsLayer = layers.getValue(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE)
        val dto = SettingsDto.fromSettingsValues(settingsLayer)
        persistenceService.persistSettings(dto)
    }

    companion object {
        /**
         * Coerces a string value (from env vars or CLI) to the typed value
         * expected by the registry entry.
         */
        internal fun coerceStringValue(entry: SettingItem, rawValue: String): Any? {
            return when (entry.type) {
                Boolean::class -> rawValue.toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("'$rawValue' is not a valid boolean")
                String::class -> rawValue
                FilePath::class -> if (rawValue.isBlank()) null else FilePath(rawValue)
                List::class -> rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                StringFromDropDown::class -> {
                    val declaration = entry.defaultValue as StringFromDropDown
                    if (!declaration.isAllowed(rawValue)) {
                        val allowed = declaration.dropDownOptions.joinToString(", ") { it.id }
                        throw IllegalArgumentException("'$rawValue' is not an allowed value (allowed: $allowed)")
                    }
                    rawValue
                }
                else -> throw IllegalArgumentException("Unsupported type: ${entry.type.simpleName}")
            }
        }
    }
}
