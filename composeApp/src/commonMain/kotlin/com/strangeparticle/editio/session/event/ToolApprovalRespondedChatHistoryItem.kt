package com.strangeparticle.editio.session.event

internal data class ToolApprovalRespondedChatHistoryItem(
    val toolCallId: String,
    val approved: Boolean,
) : ChatHistoryItem
