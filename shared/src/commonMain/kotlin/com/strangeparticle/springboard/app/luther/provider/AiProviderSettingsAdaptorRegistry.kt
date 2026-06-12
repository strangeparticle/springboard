package com.strangeparticle.springboard.app.luther.provider

import com.strangeparticle.springboard.app.luther.provider.anthropic.AnthropicProviderSettingsAdaptor
import com.strangeparticle.springboard.app.luther.provider.openai.OpenAiProviderSettingsAdaptor

internal object AiProviderSettingsAdaptorRegistry {
    private val adaptors: List<AiProviderSettingsAdaptor> =
        listOf(OpenAiProviderSettingsAdaptor, AnthropicProviderSettingsAdaptor)

    fun all(): List<AiProviderSettingsAdaptor> = adaptors
    fun byId(id: String): AiProviderSettingsAdaptor? = adaptors.firstOrNull { it.providerId == id }
    fun allSettingsItems() = adaptors.flatMap { it.settingsItems() }
}
