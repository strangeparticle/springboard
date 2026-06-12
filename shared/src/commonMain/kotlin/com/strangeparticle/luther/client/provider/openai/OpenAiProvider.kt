package com.strangeparticle.luther.client.provider.openai

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.client.provider.ProviderConfig
import io.ktor.client.HttpClient

internal object OpenAiProvider : AiProvider {
    override val id = "openai"
    override val displayName = "OpenAI"

    private fun config(config: ProviderConfig) = config as OpenAiConfig

    override fun isConfigured(config: ProviderConfig): Boolean =
        config(config).apiKey.isNotBlank()

    override fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient {
        val openai = config(config)
        return AiProviderClientOpenAi(httpClient = httpClient, apiKeyProvider = { openai.apiKey })
    }

    override fun orderModelsForPicker(models: List<AiProviderClientModelInfo>): List<AiProviderClientModelInfo> {
        val tooled = models.filter { it.supportsToolCalling }
        val preferred = preferredModelIds().mapNotNull { id -> tooled.firstOrNull { it.id == id } }
        val remainder = tooled.filterNot { it.id in preferredModelIds().toSet() }
        return preferred + remainder
    }

    private fun preferredModelIds(): List<String> = listOf("gpt-5", "gpt-4.1")
}
