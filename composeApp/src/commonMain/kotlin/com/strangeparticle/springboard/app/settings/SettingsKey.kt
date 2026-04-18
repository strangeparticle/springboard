package com.strangeparticle.springboard.app.settings

/**
 * The authoritative list of all settings the application knows about.
 * Getters and registry lookups take enum values as keys, not strings.
 *
 * All external names (env var, CLI flag, URL param, JSON key) are derived
 * from the enum name by [SettingsKeyNaming].
 */
enum class SettingsKey {
    STARTUP_TABS,
    OPEN_URLS_IN_NEW_WINDOW_SINGLE,
    OPEN_URLS_IN_NEW_WINDOW_MULTIPLE,
    SURFACE_APPLESCRIPT_ERRORS,
    RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION,
    RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION,
    ACTIVE_BRAND,
    ;
}
