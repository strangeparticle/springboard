package com.strangeparticle.springboard.app.settings

/**
 * Derives all external names for a [SettingsKey] from its enum name.
 * The enum name (UPPER_SNAKE_CASE) is the single source of truth.
 */
object SettingsKeyNaming {

    /** `STARTUP_TABS` → `startup-tabs` */
    fun urlParamName(key: SettingsKey): String =
        key.name.lowercase().replace('_', '-')

    /** `STARTUP_TABS` → `--startup-tabs` */
    fun cliFlag(key: SettingsKey): String =
        "--${urlParamName(key)}"

    /** `STARTUP_TABS` → `SPRINGBOARD_STARTUP_TABS` */
    fun envVarName(key: SettingsKey): String =
        "SPRINGBOARD_${key.name}"

    /** `STARTUP_TABS` → `startupTabs` */
    fun jsonKey(key: SettingsKey): String =
        key.name.lowercase().split("_").mapIndexed { index, word ->
            if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }.joinToString("")
}
