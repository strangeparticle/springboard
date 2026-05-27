package com.strangeparticle.luther.session.event

import com.strangeparticle.luther.toolcall.ToolCall

internal data class ToolCallStartedChatHistoryItem(val toolCall: ToolCall) : ChatHistoryItem
