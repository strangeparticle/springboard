package com.strangeparticle.luther.client.provider

import io.ktor.client.HttpClient

/**
 * Stateless model/provider discovery. Needs only {providerId, ProviderConfig} — no
 * running session. Populates the provider and model dropdowns for the host's settings
 * UI and the in-pane model picker.
 *
 * [httpClient] is optional: when null, a default ktor client is created lazily and shared.
 */
class LutherProviderCatalog(
    private val providers: List<AiProvider>,
    httpClient: HttpClient?,
) {
    private val client: HttpClient by lazy { httpClient ?: HttpClient() }

    fun availableProviders(): List<Choice> =
        providers.map { Choice(it.id, it.displayName) }

    suspend fun availableModels(providerId: String, config: ProviderConfig): List<Choice> {
        val provider = providers.firstOrNull { it.id == providerId } ?: return emptyList()
        if (!provider.isConfigured(config)) return emptyList()
        val models = provider.createClient(config, client).listModels()
        return provider.orderModelsForPicker(models).map { Choice(it.id, it.displayName ?: it.id) }
    }
}
