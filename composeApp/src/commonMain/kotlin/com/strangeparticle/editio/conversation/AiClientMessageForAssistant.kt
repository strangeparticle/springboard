package com.strangeparticle.editio.conversation

import com.strangeparticle.editio.toolcall.ToolCall

internal data class AiClientMessageForAssistant(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
) : AiClientMessage()
