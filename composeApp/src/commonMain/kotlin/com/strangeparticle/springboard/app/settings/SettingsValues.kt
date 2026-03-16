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
 *
 * TODO: If a future nullable setting needs to distinguish "not provided"
 * from "explicitly set to null", this structure will need an additional
 * presence-tracking mechanism.
 */
data class SettingsValues(
    val startupSpringboard: FilePath? = null,
    val openUrlsInNewWindowSingle: Boolean? = null,
    val openUrlsInNewWindowMultiple: Boolean? = null,
    val surfaceApplescriptErrors: Boolean? = null,
    val resetKeynavAfterKeynavActivation: Boolean? = null,
    val resetKeynavAfterGridnavActivation: Boolean? = null,
) {
    /** Returns true if this source provided a non-null value for the given key. */
    fun isSet(key: SettingsKey): Boolean = get(key) != null

    /** Returns the value for the given key, or null if not set. */
    fun get(key: SettingsKey): Any? = when (key) {
        SettingsKey.STARTUP_SPRINGBOARD -> startupSpringboard
        SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE -> openUrlsInNewWindowSingle
        SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE -> openUrlsInNewWindowMultiple
        SettingsKey.SURFACE_APPLESCRIPT_ERRORS -> surfaceApplescriptErrors
        SettingsKey.RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION -> resetKeynavAfterKeynavActivation
        SettingsKey.RESET_KEYNAV_AFTER_GRIDNAV_ACTIVATION -> resetKeynavAfterGridnavActivation
    }

    /** Returns a copy with the given key/value set. */
    fun withSetting(key: SettingsKey, value: Any?): SettingsValues {
        return when (key) {
            SettingsKey.STARTUP_SPRINGBOARD -> copy(startupSpringboard = value as FilePath?)
            SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE -> copy(openUrlsInNewWindowSingle = value as Boolean?)
            SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE -> copy(openUrlsInNewWindowMultiple = value as Boolean?)
            SettingsKey.SURFACE_APPLESCRIPT_ERRORS -> copy(surfaceApplescriptErrors = value as Boolean?)
            SettingsKey.RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION -> copy(resetKeynavAfterKeynavActivation = value as Boolean?)
            SettingsKey.RESET_KEYNAV_AFTER_GRIDNAV_ACTIVATION -> copy(resetKeynavAfterGridnavActivation = value as Boolean?)
        }
    }

    companion object {
        /**
         * The set of property names on [SettingsValues] that correspond to settings.
         * Used by synchronization tests to verify alignment with the registry.
         */
        val settingsPropertyNames: Set<String> = setOf(
            "startupSpringboard",
            "openUrlsInNewWindowSingle",
            "openUrlsInNewWindowMultiple",
            "surfaceApplescriptErrors",
            "resetKeynavAfterKeynavActivation",
            "resetKeynavAfterGridnavActivation",
        )
    }
}
