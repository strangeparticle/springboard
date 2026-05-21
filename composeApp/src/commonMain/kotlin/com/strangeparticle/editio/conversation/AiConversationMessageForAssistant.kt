package com.strangeparticle.editio.conversation

import com.strangeparticle.editio.toolcall.ToolCall

internal data class AiConversationMessageForAssistant(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
) : AiConversationMessage()
