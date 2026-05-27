package com.strangeparticle.luther.toolcall

import com.strangeparticle.luther.conversation.AiConversationMessage

internal data class ToolCallProviderClientMessage(
    val toolCallId: String,
    val content: String,
) : AiConversationMessage()
