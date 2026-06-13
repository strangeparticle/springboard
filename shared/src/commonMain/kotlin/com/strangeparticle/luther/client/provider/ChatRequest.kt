package com.strangeparticle.luther.client.provider

internal data class ChatRequest(
    val modelId: String,
    val systemPrompt: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>,
    val maxTokens: Int? = null,
)
