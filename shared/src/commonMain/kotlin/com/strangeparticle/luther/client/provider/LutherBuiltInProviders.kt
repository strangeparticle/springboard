package com.strangeparticle.luther.client.provider

import com.strangeparticle.luther.client.provider.anthropic.AnthropicProvider
import com.strangeparticle.luther.client.provider.openai.OpenAiProvider

/** The providers luther ships with. Hosts pass `LutherBuiltInProviders.all() + customProviders`
 *  into the catalog and session factory — registration is a constructor argument, not a global. */
internal object LutherBuiltInProviders {
    fun all(): List<AiProvider> = listOf(OpenAiProvider, AnthropicProvider)
}
