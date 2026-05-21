package com.strangeparticle.editio.session.event

internal data class ToolCallFailedAiChatEvent(
    val toolCallId: String,
    val providerContent: String,
    val message: String,
) : AiChatEvent
