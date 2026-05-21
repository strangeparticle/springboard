package com.strangeparticle.springboard.app.settings

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

/**
 * Resolves effective values across layered config sources (defaults, persisted,
 * env vars, CLI/URL params, session) and brokers user mutations through the
 * persistence service.
 *
 * Each [SettingsSource] in [PRECEDENCE_CHAIN] backs one [SettingsValues] layer.
 * Reads walk highest-to-lowest priority; the first source that provides a value
 * for the given item wins, with the item's `defaultValue` as the final fallback.
 */
class SettingsManager(
    val runtimeEnvironment: RuntimeEnvironment,
    val registry: SettingsRegistry,
    private val persistenceService: PersistenceService,
) {

    private val layers: MutableMap<SettingsSource, SettingsValues> =
        mutableMapOf<SettingsSource, SettingsValues>().apply {
            for (source in PRECEDENCE_CHAIN) {
                put(source, SettingsValues())
            }
        }

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

    // -- Source Tracking --

    fun getEffectiveSource(item: SettingsItem<*>): SettingsSource {
        for (source in PRECEDENCE_CHAIN) {
            val layer = layers[source] ?: continue
            if (layer.isSet(item)) return source
        }
        return SettingsSource.APP_DEFAULT
    }

    fun getSource(item: SettingsItem<*>): SettingsSource = getEffectiveSource(item)

    fun getValueFromSource(item: SettingsItem<*>, source: SettingsSource): Any? {
        val layer = layers[source] ?: return null
        return if (layer.isSet(item)) layer.getById(item.id) else null
    }

    /** True if the user has explicitly set this setting (session or persistence layer). */
    fun isOverridden(item: SettingsItem<*>): Boolean {
        val source = getEffectiveSource(item)
        return source == SettingsSource.USER_SETTINGS_FROM_SESSION ||
            source == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE
    }

    // -- Value Resolution --

    fun <T : Any> resolveValue(item: SettingsItem<T>): T {
        for (source in PRECEDENCE_CHAIN) {
            val layer = layers[source] ?: continue
            if (layer.isSet(item)) {
                val value = layer.get(item)
                if (value != null) return value
            }
        }
        return item.defaultValue
    }

    fun applicableSettings(): List<SettingsItem<*>> = registry.forEnvironment(runtimeEnvironment)

    // -- User Settings Mutation --

    fun <T : Any> setUserSetting(item: SettingsItem<T>, value: T?) {
        layers[SettingsSource.USER_SETTINGS_FROM_SESSION] =
            layers.getValue(SettingsSource.USER_SETTINGS_FROM_SESSION).withSetting(item, value)
        layers[SettingsSource.USER_SETTINGS_FROM_PERSISTENCE] =
            layers.getValue(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE).withSetting(item, value)
        persistSettingsLayer()
    }

    fun clearUserSetting(item: SettingsItem<*>) {
        layers[SettingsSource.USER_SETTINGS_FROM_SESSION] =
            layers.getValue(SettingsSource.USER_SETTINGS_FROM_SESSION).withRawSetting(item.id, null)
        layers[SettingsSource.USER_SETTINGS_FROM_PERSISTENCE] =
            layers.getValue(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE).withRawSetting(item.id, null)
        persistSettingsLayer()
    }

    // -- Private Loading --

    private fun initDefaultSettings() {
        var defaults = SettingsValues()
        for (item in registry.forEnvironment(runtimeEnvironment)) {
            defaults = defaults.withRawSetting(item.id, item.defaultValue)
        }
        layers[SettingsSource.APP_DEFAULT] = defaults
    }

    private fun loadPersistedSettings() {
        val dto = persistenceService.loadSettings()
        if (dto != null) {
            layers[SettingsSource.USER_SETTINGS_FROM_PERSISTENCE] = dto.toSettingsValues(registry)
        }
    }

    private fun loadEnvironmentVariables(envVars: Map<String, String>) {
        val applicable = registry.forEnvironment(runtimeEnvironment).toSet()
        var envValues = SettingsValues()

        for ((envVarName, rawValue) in envVars) {
            val item = registry.byEnvVarName(envVarName) ?: continue
            if (item !in applicable) continue

            try {
                val typedValue = item.coerceFromString(rawValue)
                envValues = envValues.withRawSetting(item.id, typedValue)
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid environment variable '$envVarName': ${e.message}")
            }
        }

        layers[SettingsSource.ENVIRONMENT_VARIABLE] = envValues
    }

    private fun loadCommandLineArgs(args: List<String>) {
        val applicable = registry.forEnvironment(runtimeEnvironment).toSet()
        var cliValues = SettingsValues()

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            if (!arg.startsWith("--")) {
                i++
                continue
            }

            val item = registry.byUrlParamName(arg.removePrefix("--"))
            if (item == null || item !in applicable) {
                i++
                continue
            }

            try {
                cliValues = cliValues.parseCliValue(item, args, i) { consumedExtra -> if (consumedExtra) i++ }
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid CLI parameter '$arg': ${e.message}")
            }
            i++
        }

        layers[SettingsSource.CLI_FLAG] = cliValues
    }

    private fun SettingsValues.parseCliValue(
        item: SettingsItem<*>,
        args: List<String>,
        currentIndex: Int,
        onConsumedNext: (Boolean) -> Unit,
    ): SettingsValues {
        // Boolean flags don't take a following value — their presence means `true`.
        if (item.valueClass == Boolean::class) {
            return withRawSetting(item.id, true)
        }
        val nextArg = args.getOrNull(currentIndex + 1)
        if (nextArg == null || nextArg.startsWith("--")) {
            ToastBroadcaster.warning("CLI parameter '${args[currentIndex]}' requires a value")
            return this
        }
        val typedValue = item.coerceFromString(nextArg)
        onConsumedNext(true)
        return withRawSetting(item.id, typedValue)
    }

    private fun loadUrlParams(urlParams: Map<String, String>) {
        val applicable = registry.forEnvironment(runtimeEnvironment).toSet()
        var urlValues = SettingsValues()

        for ((paramName, rawValue) in urlParams) {
            val item = registry.byUrlParamName(paramName) ?: continue
            if (item !in applicable) continue

            try {
                val typedValue = item.coerceFromString(rawValue)
                urlValues = urlValues.withRawSetting(item.id, typedValue)
            } catch (e: Exception) {
                ToastBroadcaster.warning("Invalid URL parameter '$paramName': ${e.message}")
            }
        }

        layers[SettingsSource.URL_PARAM] = urlValues
    }

    private fun persistSettingsLayer() {
        val settingsLayer = layers.getValue(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE)
        val dto = SettingsDto.fromSettingsValues(settingsLayer, registry)
        persistenceService.persistSettings(dto)
    }
}
