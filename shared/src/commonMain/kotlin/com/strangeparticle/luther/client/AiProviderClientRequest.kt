package com.strangeparticle.luther.client

import com.strangeparticle.luther.client.provider.ChatMessage
import com.strangeparticle.luther.client.provider.ToolDefinition

/** A provider-neutral request for a single model turn. */
internal data class AiProviderClientRequest(
    val modelId: String,
    val systemPrompt: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>,
    val maxTokens: Int? = null, // required by Anthropic; optional for OpenAI
)
