package com.strangeparticle.luther.session.event

import com.strangeparticle.luther.client.provider.ToolCall

internal data class ToolCallStartedChatHistoryItem(val toolCall: ToolCall) : ChatHistoryItem
