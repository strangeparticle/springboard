package com.strangeparticle.springboard.app.settings

import kotlin.reflect.KClass

/**
 * Metadata for a single setting. Sealed so that exhaustive `when` checks
 * cover all subclasses without an `else` branch.
 */
sealed class SettingItem(
    val key: SettingsKey,
    val type: KClass<*>,
    val defaultValue: Any?,
    val displayName: String,
    val description: String,
    val envVarName: String,
    val cliParamName: String,
) {
    /** A setting that applies to all runtime environments. */
    class General(
        key: SettingsKey,
        type: KClass<*>,
        defaultValue: Any?,
        displayName: String,
        description: String,
        envVarName: String,
        cliParamName: String,
    ) : SettingItem(key, type, defaultValue, displayName, description, envVarName, cliParamName)

    /** A setting that applies only to specific desktop runtime environments. */
    class Desktop(
        key: SettingsKey,
        type: KClass<*>,
        defaultValue: Any?,
        displayName: String,
        description: String,
        envVarName: String,
        cliParamName: String,
        val runtimeEnvironments: List<RuntimeEnvironment>,
    ) : SettingItem(key, type, defaultValue, displayName, description, envVarName, cliParamName)
}
