package com.strangeparticle.springboard.app.settings

/**
 * Represents the configuration sources from which setting values may originate.
 */
enum class SettingsSource {
    APP_DEFAULT,
    ENVIRONMENT_VARIABLE,
    USER_SETTINGS_FROM_PERSISTENCE,
    CLI_FLAG,
    URL_PARAM,
    USER_SETTINGS_FROM_SESSION,
    ;

    fun displayLabel(runtimeEnvironment: RuntimeEnvironment): String = when (this) {
        APP_DEFAULT -> "Default"
        USER_SETTINGS_FROM_SESSION -> "User (session)"
        USER_SETTINGS_FROM_PERSISTENCE -> "User (saved)"
        ENVIRONMENT_VARIABLE -> "Env var"
        CLI_FLAG -> "CLI flag"
        URL_PARAM -> "URL param"
    }
}

/**
 * The fixed precedence chain, ordered from highest to lowest precedence.
 * Resolution walks this list top-down; the first source that provides a
 * non-null value for a given key wins.
 *
 * USER_SETTINGS_FROM_SESSION is highest because in-session user choices
 * must always take immediate effect, overriding everything.
 * CLI_FLAG sits above USER_SETTINGS_FROM_PERSISTENCE so that deployment
 * flags win over saved state at startup. Once the user makes a change
 * during the session, it goes into SESSION and outranks CLI_FLAG.
 */
val PRECEDENCE_CHAIN: List<SettingsSource> = listOf(
    SettingsSource.USER_SETTINGS_FROM_SESSION,
    SettingsSource.URL_PARAM,
    SettingsSource.CLI_FLAG,
    SettingsSource.USER_SETTINGS_FROM_PERSISTENCE,
    SettingsSource.ENVIRONMENT_VARIABLE,
    SettingsSource.APP_DEFAULT,
)
