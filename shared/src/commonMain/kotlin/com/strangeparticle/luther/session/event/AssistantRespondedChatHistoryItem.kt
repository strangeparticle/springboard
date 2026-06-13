package com.strangeparticle.luther.session.event

import com.strangeparticle.luther.client.provider.ToolCall

internal data class AssistantRespondedChatHistoryItem(
    val text: String?,
    val toolCalls: List<ToolCall>,
) : ChatHistoryItem
