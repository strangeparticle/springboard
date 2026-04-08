package com.strangeparticle.springboard.app.settings

import com.strangeparticle.springboard.app.settings.persistence.SettingsPersistenceManager
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto
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
    private val runtimeEnvironment: RuntimeEnvironment,
    private val persistenceManager: SettingsPersistenceManager,
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
     * 4. Load command-line parameters.
     */
    fun loadSettingsAtStartup(
        environmentVariables: Map<String, String> = emptyMap(),
        commandLineArgs: List<String> = emptyList(),
    ) {
        initDefaultSettings()
        loadUserSettings()
        loadEnvironmentVariables(environmentVariables)
        loadCommandLineArgs(commandLineArgs)
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
    fun getSource(key: SettingsKey): SettingsSource {
        for (source in PRECEDENCE_CHAIN.reversed()) {
            val layer = layers[source] ?: continue
            if (layer.isSet(key)) {
                return source
            }
        }
        return SettingsSource.APP_DEFAULT
    }

    /**
     * Returns true if the setting has been overridden by a source higher
     * than [SettingsSource.USER_SETTINGS] (i.e., env var or CLI param).
     */
    fun isOverridden(key: SettingsKey): Boolean {
        val source = getSource(key)
        return source == SettingsSource.ENVIRONMENT_VARIABLE || source == SettingsSource.COMMAND_LINE
    }

    /**
     * Returns the resolved value for the given key, applying the precedence chain.
     * The first source (highest precedence) that provides a non-null value wins.
     */
    fun resolveValue(key: SettingsKey): Any? {
        for (source in PRECEDENCE_CHAIN.reversed()) {
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
     * Updates a user setting value and persists it immediately.
     */
    fun setUserSetting(key: SettingsKey, value: Any?) {
        val entry = SettingsRegistry.require(key)
        if (entry.type == StringFromDropDown::class && value is String) {
            val declaration = entry.defaultValue as StringFromDropDown
            require(declaration.isAllowed(value)) {
                "'$value' is not an allowed value for $key"
            }
        }
        layers[SettingsSource.USER_SETTINGS] = layers.getValue(SettingsSource.USER_SETTINGS).withSetting(key, value)
        persistUserSettings()
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

    private fun loadUserSettings() {
        val dto = persistenceManager.loadDto()
        if (dto != null) {
            layers[SettingsSource.USER_SETTINGS] = dto.toSettingsValues()
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

            val entry = SettingsRegistry.findByCliParamName(arg)
            if (entry == null || entry.key !in applicableKeys) {
                i++
                continue
            }

            try {
                when (entry.type) {
                    Boolean::class -> {
                        cliValues = cliValues.withSetting(entry.key, true)
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

        layers[SettingsSource.COMMAND_LINE] = cliValues
    }

    private fun persistUserSettings() {
        val userLayer = layers.getValue(SettingsSource.USER_SETTINGS)
        val dto = UserSettingsDto.fromSettingsValues(userLayer)
        persistenceManager.saveDto(dto)
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
