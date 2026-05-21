package com.strangeparticle.springboard.app.settings

/**
 * Derives external names for a [SettingsItem] from its [SettingsItem.id].
 *
 * Item ids are snake_case (sometimes with dots, e.g. `ai.openai.api_key`).
 * For env vars / URL params / CLI flags the dots collapse to underscores /
 * hyphens; for JSON the id is used verbatim as the persistence key.
 */
object SettingsKeyNaming {

    /** `startup_tabs` → `startup-tabs`; `ai.openai.api_key` → `ai-openai-api-key` */
    fun urlParamName(item: SettingsItem<*>): String =
        item.id.replace('_', '-').replace('.', '-')

    /** `startup_tabs` → `--startup-tabs` */
    fun cliFlag(item: SettingsItem<*>): String = "--${urlParamName(item)}"

    /**
     * `startup_tabs` → `SPRINGBOARD_STARTUP_TABS`; provider items override
     * via [SettingsItem.envVarNameOverride] to use a non-prefixed name like
     * `OPENAI_API_KEY`.
     */
    fun envVarName(item: SettingsItem<*>): String =
        item.envVarNameOverride
            ?: "SPRINGBOARD_${item.id.uppercase().replace('.', '_')}"

    /** The JSON persistence key is just the id, verbatim. */
    fun jsonKey(item: SettingsItem<*>): String = item.id
}
