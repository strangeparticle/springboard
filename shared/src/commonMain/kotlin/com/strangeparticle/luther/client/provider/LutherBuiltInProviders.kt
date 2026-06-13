package com.strangeparticle.luther.client.provider

import com.strangeparticle.luther.client.provider.anthropic.AnthropicProvider
import com.strangeparticle.luther.client.provider.openai.OpenAiProvider
import io.ktor.client.HttpClient

/** The providers luther ships with. Hosts pass `LutherBuiltInProviders.all(httpClient) + customProviders`
 *  into the catalog and session factory — registration is a constructor argument, not a global. */
internal object LutherBuiltInProviders {
    fun all(httpClient: HttpClient): List<AiProvider> = listOf(OpenAiProvider(httpClient), AnthropicProvider(httpClient))
}
