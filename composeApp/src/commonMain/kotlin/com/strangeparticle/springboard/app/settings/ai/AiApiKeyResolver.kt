package com.strangeparticle.springboard.app.settings.ai

import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager

/**
 * Resolves the API key for an AI provider with env-var-first precedence.
 *
 * For each provider, the canonical environment variable (`OPENAI_API_KEY` or
 * `ANTHROPIC_API_KEY`) takes precedence over the persisted user setting.
 * Empty values from either source are treated as unset so that an empty env
 * var doesn't mask a valid persisted key.
 *
 * This bypasses the normal `SettingsManager` env-var precedence chain because:
 *  - The settings system uses `SPRINGBOARD_*` prefixes for env var names; the
 *    canonical AI keys (`OPENAI_API_KEY`, `ANTHROPIC_API_KEY`) are conventions
 *    set by the providers themselves.
 *  - The settings system treats any non-null env value as set; here we want to
 *    treat empty strings as unset so they don't mask a usable persisted value.
 */
class AiApiKeyResolver(
    private val settingsManager: SettingsManager,
    private val environmentVariables: Map<String, String>,
) {

    /**
     * Returns the resolved API key for [provider], or null if none is available.
     * For [AiProvider.None] this always returns null.
     */
    fun resolve(provider: AiProvider): String? {
        val envName = envVarNameFor(provider) ?: return null
        val envValue = environmentVariables[envName].nonBlankOrNull()
        if (envValue != null) return envValue
        val settingsKey = settingsKeyFor(provider) ?: return null
        return settingsManager.getString(settingsKey).nonBlankOrNull()
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun envVarNameFor(provider: AiProvider): String? = when (provider) {
        AiProvider.OpenAi -> "OPENAI_API_KEY"
        AiProvider.Anthropic -> "ANTHROPIC_API_KEY"
        AiProvider.None -> null
    }

    private fun settingsKeyFor(provider: AiProvider): SettingsKey? = when (provider) {
        AiProvider.OpenAi -> SettingsKey.AI_OPENAI_API_KEY
        AiProvider.Anthropic -> SettingsKey.AI_ANTHROPIC_API_KEY
        AiProvider.None -> null
    }
}
