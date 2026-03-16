package com.strangeparticle.springboard.app.settings

/**
 * The authoritative list of all settings the application knows about.
 * Getters and registry lookups take enum values as keys, not strings.
 *
 * The [jsonKey] property derives the camelCase JSON key from the enum name.
 * For example, `SURFACE_APPLESCRIPT_ERRORS` becomes `surfaceApplescriptErrors`.
 */
enum class SettingsKey {
    STARTUP_SPRINGBOARD,
    OPEN_URLS_IN_NEW_WINDOW_SINGLE,
    OPEN_URLS_IN_NEW_WINDOW_MULTIPLE,
    SURFACE_APPLESCRIPT_ERRORS,
    RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION,
    RESET_KEYNAV_AFTER_GRIDNAV_ACTIVATION,
    ;

    /** The camelCase JSON key derived from this enum name. */
    val jsonKey: String by lazy {
        name.lowercase().split("_").mapIndexed { index, word ->
            if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
}
