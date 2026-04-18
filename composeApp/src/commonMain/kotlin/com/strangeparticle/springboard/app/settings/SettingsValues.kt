package com.strangeparticle.springboard.app.settings

/**
 * WARNING: This class must remain in sync with [SettingsRegistry].
 *
 * Every setting registered in [SettingsRegistry] must have a corresponding
 * typed property here, and vice versa. If you add a setting to the registry,
 * add a matching property here. If you add a property here, add a matching
 * registry entry. Unit tests enforce this contract.
 *
 * Holds the values for every setting as typed properties. Each instance
 * represents the values from one config source layer.
 *
 * A null property means this source did not provide a value for that setting.
 */
data class SettingsValues(
    val startupTabs: List<String>? = null,
    val openUrlsInNewWindowSingle: Boolean? = null,
    val openUrlsInNewWindowMultiple: Boolean? = null,
    val surfaceApplescriptErrors: Boolean? = null,
    val resetKeyNavAfterKeyNavActivation: Boolean? = null,
    val resetKeyNavAfterGridNavActivation: Boolean? = null,
    val activeBrand: String? = null,
) {
    fun isSet(key: SettingsKey): Boolean = get(key) != null

    fun get(key: SettingsKey): Any? = when (key) {
        SettingsKey.STARTUP_TABS -> startupTabs
        SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE -> openUrlsInNewWindowSingle
        SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE -> openUrlsInNewWindowMultiple
        SettingsKey.SURFACE_APPLESCRIPT_ERRORS -> surfaceApplescriptErrors
        SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION -> resetKeyNavAfterKeyNavActivation
        SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION -> resetKeyNavAfterGridNavActivation
        SettingsKey.ACTIVE_BRAND -> activeBrand
    }

    @Suppress("UNCHECKED_CAST")
    fun withSetting(key: SettingsKey, value: Any?): SettingsValues {
        return when (key) {
            SettingsKey.STARTUP_TABS -> copy(startupTabs = value as List<String>?)
            SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE -> copy(openUrlsInNewWindowSingle = value as Boolean?)
            SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE -> copy(openUrlsInNewWindowMultiple = value as Boolean?)
            SettingsKey.SURFACE_APPLESCRIPT_ERRORS -> copy(surfaceApplescriptErrors = value as Boolean?)
            SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION -> copy(resetKeyNavAfterKeyNavActivation = value as Boolean?)
            SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION -> copy(resetKeyNavAfterGridNavActivation = value as Boolean?)
            SettingsKey.ACTIVE_BRAND -> copy(activeBrand = value as String?)
        }
    }

    companion object {
        val settingsPropertyNames: Set<String> = setOf(
            "startupTabs",
            "openUrlsInNewWindowSingle",
            "openUrlsInNewWindowMultiple",
            "surfaceApplescriptErrors",
            "resetKeyNavAfterKeyNavActivation",
            "resetKeyNavAfterGridNavActivation",
            "activeBrand",
        )
    }
}
