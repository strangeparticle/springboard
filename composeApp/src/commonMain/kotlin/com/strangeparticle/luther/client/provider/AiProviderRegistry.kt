package com.strangeparticle.luther.client.provider

import com.strangeparticle.luther.client.provider.anthropic.AnthropicProvider
import com.strangeparticle.luther.client.provider.openai.OpenAiProvider

/**
 * Compile-time-known list of [AiProvider] implementations. Adding a new provider
 * is one line here plus the new provider directory.
 */
internal object AiProviderRegistry {
    private val providers: List<AiProvider> = listOf(
        OpenAiProvider,
        AnthropicProvider,
    )

    fun all(): List<AiProvider> = providers
    fun byId(id: String): AiProvider? = providers.firstOrNull { it.id == id }
}
