package com.strangeparticle.luther.client

import com.strangeparticle.luther.conversation.AiConversationMessage
import com.strangeparticle.luther.toolcall.AiToolCallDefinition

/** A provider-neutral request for a single model turn. */
internal data class AiProviderClientRequest(
    val modelId: String,
    val systemPrompt: String,
    val history: List<AiConversationMessage>,
    val tools: List<AiToolCallDefinition>,
    val maxTokens: Int? = null, // required by Anthropic; optional for OpenAI
)
