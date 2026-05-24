package com.strangeparticle.editio.session.event

import com.strangeparticle.editio.toolcall.ToolCall

internal data class ToolCallStartedChatHistoryItem(val toolCall: ToolCall) : ChatHistoryItem
