package com.strangeparticle.luther.conversation

import com.strangeparticle.luther.toolcall.ToolCall

internal data class AiConversationMessageForAssistant(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
) : AiConversationMessage()
