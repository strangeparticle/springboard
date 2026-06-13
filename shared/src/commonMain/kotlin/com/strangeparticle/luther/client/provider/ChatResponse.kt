package com.strangeparticle.luther.client.provider

data class ChatResponse(
    val text: String?,
    val toolCalls: List<ToolCall>,
    val stopReason: StopReason,
)
