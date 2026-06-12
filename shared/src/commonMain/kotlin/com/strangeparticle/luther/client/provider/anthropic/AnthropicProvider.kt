package com.strangeparticle.luther.client.provider.anthropic

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.client.provider.ProviderConfig
import io.ktor.client.HttpClient

internal object AnthropicProvider : AiProvider {
    override val id = "anthropic"
    override val displayName = "Anthropic"

    private fun config(config: ProviderConfig) = config as AnthropicConfig

    override fun isConfigured(config: ProviderConfig): Boolean =
        config(config).apiKey.isNotBlank()

    override fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient {
        val anthropic = config(config)
        return AiProviderClientAnthropic(httpClient = httpClient, apiKeyProvider = { anthropic.apiKey })
    }

    override fun orderModelsForPicker(models: List<AiProviderClientModelInfo>): List<AiProviderClientModelInfo> {
        val tooled = models.filter { it.supportsToolCalling }
        val preferred = preferredModelIds().mapNotNull { id -> tooled.firstOrNull { it.id == id } }
        val remainder = tooled.filterNot { it.id in preferredModelIds().toSet() }
        return preferred + remainder
    }

    private fun preferredModelIds(): List<String> = listOf("claude-sonnet-4-6", "claude-3-5-sonnet-latest")
}
