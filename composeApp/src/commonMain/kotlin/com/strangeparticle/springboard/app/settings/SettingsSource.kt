package com.strangeparticle.springboard.app.settings

/**
 * Represents the configuration sources from which setting values may originate.
 */
enum class SettingsSource {
    APP_DEFAULT,
    USER_SETTINGS,
    ENVIRONMENT_VARIABLE,
    COMMAND_LINE,
}

/**
 * The fixed precedence chain, ordered from lowest to highest precedence.
 * Each source overrides the one before it on a per-setting basis.
 */
val PRECEDENCE_CHAIN: List<SettingsSource> = listOf(
    SettingsSource.APP_DEFAULT,
    SettingsSource.USER_SETTINGS,
    SettingsSource.ENVIRONMENT_VARIABLE,
    SettingsSource.COMMAND_LINE,
)
