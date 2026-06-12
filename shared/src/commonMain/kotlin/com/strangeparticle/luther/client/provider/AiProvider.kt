package com.strangeparticle.luther.client.provider

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import io.ktor.client.HttpClient

/**
 * Framework contract for an AI provider integration. Providers live whole inside
 * luther-core and know nothing about the host's settings system: the host supplies
 * a typed [ProviderConfig] and luther supplies the [HttpClient].
 *
 * Each provider casts the marker [ProviderConfig] to its own concrete config type
 * (e.g. AnthropicConfig) at the top of each method.
 */
internal interface AiProvider {
    val id: String
    val displayName: String

    /** True if [config] carries everything needed to construct a working client. */
    fun isConfigured(config: ProviderConfig): Boolean

    /** Build a vendor-aware client from typed [config], using luther's [httpClient]. */
    fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient

    /** Tool-calling filter + preferred-first ordering for the model picker. */
    fun orderModelsForPicker(models: List<AiProviderClientModelInfo>): List<AiProviderClientModelInfo>
}
