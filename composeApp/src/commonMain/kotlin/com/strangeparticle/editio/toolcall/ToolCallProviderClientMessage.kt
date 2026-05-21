package com.strangeparticle.editio.toolcall

import com.strangeparticle.editio.conversation.AiConversationMessage

internal data class ToolCallProviderClientMessage(
    val toolCallId: String,
    val content: String,
) : AiConversationMessage()
