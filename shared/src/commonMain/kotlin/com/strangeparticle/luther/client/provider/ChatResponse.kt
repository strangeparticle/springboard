package com.strangeparticle.luther.client.provider

internal data class ChatResponse(
    val text: String?,
    val toolCalls: List<ToolCall>,
    val stopReason: StopReason,
)
