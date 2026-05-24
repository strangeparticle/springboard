package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.conversation.AiConversationMessage
import com.strangeparticle.editio.conversation.AiConversationMessageForAssistant
import com.strangeparticle.editio.conversation.AiConversationMessageForSystemState
import com.strangeparticle.editio.conversation.AiConversationMessageForUser
import com.strangeparticle.editio.session.event.ChatHistoryItem
import com.strangeparticle.editio.session.event.AssistantErroredChatHistoryItem
import com.strangeparticle.editio.session.event.AssistantRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.editio.session.event.StateSnapshotAddedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolApprovalRequestedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolApprovalRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallCompletedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallDeniedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallFailedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallStartedChatHistoryItem
import com.strangeparticle.editio.session.event.UserSubmittedChatHistoryItem
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage

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
