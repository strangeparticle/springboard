package com.strangeparticle.editio.session.event

internal data class ToolCallFailedChatHistoryItem(
    val toolCallId: String,
    val providerContent: String,
    val message: String,
) : ChatHistoryItem
