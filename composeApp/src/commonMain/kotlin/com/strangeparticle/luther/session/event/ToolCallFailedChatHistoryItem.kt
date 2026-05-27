package com.strangeparticle.luther.session.event

internal data class ToolCallFailedChatHistoryItem(
    val toolCallId: String,
    val providerContent: String,
    val message: String,
) : ChatHistoryItem
