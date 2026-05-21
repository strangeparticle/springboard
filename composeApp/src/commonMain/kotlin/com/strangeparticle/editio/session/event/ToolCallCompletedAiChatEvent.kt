package com.strangeparticle.editio.session.event

internal data class ToolCallCompletedAiChatEvent(
    val toolCallId: String,
    val providerContent: String,
    val transcriptOutput: String,
    val endsTurn: Boolean,
) : AiChatEvent
