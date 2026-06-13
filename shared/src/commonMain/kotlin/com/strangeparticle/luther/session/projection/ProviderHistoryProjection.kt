package com.strangeparticle.luther.session.projection

import com.strangeparticle.luther.client.provider.ChatMessage
import com.strangeparticle.luther.session.event.ChatHistoryItem
import com.strangeparticle.luther.session.event.AssistantErroredChatHistoryItem
import com.strangeparticle.luther.session.event.AssistantRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.luther.session.event.ProviderModelChangedChatHistoryItem
import com.strangeparticle.luther.session.event.StateSnapshotAddedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRequestedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallCompletedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallDeniedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallFailedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallStartedChatHistoryItem
import com.strangeparticle.luther.session.event.UserSubmittedChatHistoryItem

internal fun buildProviderHistory(events: List<ChatHistoryItem>): List<ChatMessage> = events.mapNotNull { event ->
    when (event) {
        is StateSnapshotAddedChatHistoryItem -> ChatMessage.SystemState(event.snapshotJson)
        is UserSubmittedChatHistoryItem -> ChatMessage.User(event.text)
        is AssistantRespondedChatHistoryItem -> ChatMessage.Assistant(event.text, event.toolCalls)
        is ToolCallCompletedChatHistoryItem -> ChatMessage.ToolResult(event.toolCallId, event.providerContent)
        is ToolCallFailedChatHistoryItem -> ChatMessage.ToolResult(event.toolCallId, event.providerContent)
        is AssistantErroredChatHistoryItem,
        is LocalCommandRespondedChatHistoryItem,
        is LocalCommandSubmittedChatHistoryItem,
        is ProviderModelChangedChatHistoryItem,
        is ToolApprovalRequestedChatHistoryItem,
        is ToolApprovalRespondedChatHistoryItem,
        is ToolCallDeniedChatHistoryItem,
        is ToolCallStartedChatHistoryItem -> null
    }
}
