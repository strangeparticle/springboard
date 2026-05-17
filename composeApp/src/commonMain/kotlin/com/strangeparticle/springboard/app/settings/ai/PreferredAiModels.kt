package com.strangeparticle.springboard.app.settings.ai

import com.strangeparticle.editio.client.AiClientModelInfo

internal object PreferredAiModels {
    private val preferredByProvider = mapOf(
        AiProvider.OpenAi to listOf("gpt-5", "gpt-4.1"),
        AiProvider.Anthropic to listOf("claude-sonnet-4-6", "claude-3-5-sonnet-latest"),
    )

    fun selectPreferred(provider: AiProvider, models: List<AiClientModelInfo>): AiClientModelInfo? {
        val toolModels = models.filter { it.supportsToolCalling }
        val preferredIds = preferredByProvider[provider].orEmpty()
        return preferredIds.firstNotNullOfOrNull { preferredId ->
            toolModels.firstOrNull { it.id == preferredId }
        } ?: toolModels.firstOrNull() ?: models.firstOrNull()
    }
}
