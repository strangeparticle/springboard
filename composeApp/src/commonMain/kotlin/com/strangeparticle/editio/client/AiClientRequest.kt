package com.strangeparticle.editio.client

import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.toolcall.AiToolCallDefinition

/** A provider-neutral request for a single model turn. */
internal data class AiClientRequest(
    val modelId: String,
    val systemPrompt: String,
    val history: List<AiClientMessage>,
    val tools: List<AiToolCallDefinition>,
    val maxTokens: Int? = null, // required by Anthropic; optional for OpenAI
)
