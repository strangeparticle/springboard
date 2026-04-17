package com.strangeparticle.springboard.app.settings

/**
 * Represents the configuration sources from which setting values may originate.
 */
enum class SettingsSource {
    APP_DEFAULT,
    USER_SETTINGS,
    ENVIRONMENT_VARIABLE,
    PARAMS,
}

/**
 * The fixed precedence chain, ordered from highest to lowest precedence.
 * Resolution walks this list top-down; the first source that provides a
 * non-null value for a given key wins. USER_SETTINGS has highest priority
 * so that user choices always override params and environment variables.
 */
val PRECEDENCE_CHAIN: List<SettingsSource> = listOf(
    SettingsSource.USER_SETTINGS,
    SettingsSource.PARAMS,
    SettingsSource.ENVIRONMENT_VARIABLE,
    SettingsSource.APP_DEFAULT,
)
