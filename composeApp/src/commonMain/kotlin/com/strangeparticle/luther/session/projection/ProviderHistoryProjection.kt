package com.strangeparticle.luther.session.projection

import com.strangeparticle.luther.conversation.AiConversationMessage
import com.strangeparticle.luther.conversation.AiConversationMessageForAssistant
import com.strangeparticle.luther.conversation.AiConversationMessageForSystemState
import com.strangeparticle.luther.conversation.AiConversationMessageForUser
import com.strangeparticle.luther.session.event.ChatHistoryItem
import com.strangeparticle.luther.session.event.AssistantErroredChatHistoryItem
import com.strangeparticle.luther.session.event.AssistantRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.luther.session.event.StateSnapshotAddedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRequestedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallCompletedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallDeniedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallFailedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallStartedChatHistoryItem
import com.strangeparticle.luther.session.event.UserSubmittedChatHistoryItem
import com.strangeparticle.luther.toolcall.ToolCallProviderClientMessage

internal fun buildProviderHistory(events: List<ChatHistoryItem>): List<AiConversationMessage> = events.mapNotNull { event ->
    when (event) {
        is StateSnapshotAddedChatHistoryItem -> AiConversationMessageForSystemState(event.snapshotJson)
        is UserSubmittedChatHistoryItem -> AiConversationMessageForUser(event.text)
        is AssistantRespondedChatHistoryItem -> AiConversationMessageForAssistant(event.text, event.toolCalls)
        is ToolCallCompletedChatHistoryItem -> ToolCallProviderClientMessage(event.toolCallId, event.providerContent)
        is ToolCallFailedChatHistoryItem -> ToolCallProviderClientMessage(event.toolCallId, event.providerContent)
        is AssistantErroredChatHistoryItem,
        is LocalCommandRespondedChatHistoryItem,
        is LocalCommandSubmittedChatHistoryItem,
        is ToolApprovalRequestedChatHistoryItem,
        is ToolApprovalRespondedChatHistoryItem,
        is ToolCallDeniedChatHistoryItem,
        is ToolCallStartedChatHistoryItem -> null
    }
}
