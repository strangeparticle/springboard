package com.strangeparticle.luther.session.event

internal data class ToolApprovalRespondedChatHistoryItem(
    val toolCallId: String,
    val approved: Boolean,
) : ChatHistoryItem
