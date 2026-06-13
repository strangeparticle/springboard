package com.strangeparticle.luther.conversation

import com.strangeparticle.luther.client.provider.ToolCall

internal data class AiConversationMessageForAssistant(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
) : AiConversationMessage()
