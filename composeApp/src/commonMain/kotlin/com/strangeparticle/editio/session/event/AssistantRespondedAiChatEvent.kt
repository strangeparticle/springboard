package com.strangeparticle.editio.session.event

import com.strangeparticle.editio.toolcall.ToolCall

internal data class AssistantRespondedAiChatEvent(
    val text: String?,
    val toolCalls: List<ToolCall>,
) : AiChatEvent
