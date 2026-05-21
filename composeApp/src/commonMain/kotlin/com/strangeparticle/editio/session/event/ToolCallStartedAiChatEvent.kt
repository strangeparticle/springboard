package com.strangeparticle.editio.session.event

import com.strangeparticle.editio.toolcall.ToolCall

internal data class ToolCallStartedAiChatEvent(val toolCall: ToolCall) : AiChatEvent
