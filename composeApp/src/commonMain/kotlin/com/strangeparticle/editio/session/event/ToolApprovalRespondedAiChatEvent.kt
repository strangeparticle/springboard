package com.strangeparticle.editio.session.event

internal data class ToolApprovalRespondedAiChatEvent(
    val toolCallId: String,
    val approved: Boolean,
) : AiChatEvent
