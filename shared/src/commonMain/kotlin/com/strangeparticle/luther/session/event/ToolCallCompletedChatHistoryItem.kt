package com.strangeparticle.luther.session.event

internal data class ToolCallCompletedChatHistoryItem(
    val toolCallId: String,
    val providerContent: String,
    val transcriptOutput: String,
    val endsTurn: Boolean,
) : ChatHistoryItem
