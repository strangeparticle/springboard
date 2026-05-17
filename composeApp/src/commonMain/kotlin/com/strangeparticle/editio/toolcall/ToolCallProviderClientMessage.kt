package com.strangeparticle.editio.toolcall

import com.strangeparticle.editio.conversation.AiClientMessage

internal data class ToolCallProviderClientMessage(
    val toolCallId: String,
    val content: String,
) : AiClientMessage()
