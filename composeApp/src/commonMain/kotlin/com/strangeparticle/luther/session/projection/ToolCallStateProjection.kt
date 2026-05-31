package com.strangeparticle.luther.session.projection

import com.strangeparticle.luther.session.ToolCallState
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

internal fun buildToolCallStates(events: List<ChatHistoryItem>): Map<String, ToolCallState> {
    val states = mutableMapOf<String, ToolCallState>()
    for (event in events) {
        when (event) {
            is ToolCallStartedChatHistoryItem -> states[event.toolCall.toolCallId] = ToolCallState.Pending
            is ToolApprovalRequestedChatHistoryItem -> states[event.toolCallId] = ToolCallState.ApprovalRequested
            is ToolApprovalRespondedChatHistoryItem -> states[event.toolCallId] = ToolCallState.ApprovalResponded(event.approved)
            is ToolCallCompletedChatHistoryItem -> states[event.toolCallId] = if (event.endsTurn) {
                ToolCallState.OutputDenied
            } else {
                ToolCallState.OutputAvailable(event.transcriptOutput)
            }
            is ToolCallFailedChatHistoryItem -> states[event.toolCallId] = ToolCallState.OutputError(event.message)
            is ToolCallDeniedChatHistoryItem -> states[event.toolCallId] = ToolCallState.OutputDenied
            is AssistantErroredChatHistoryItem,
            is AssistantRespondedChatHistoryItem,
            is LocalCommandRespondedChatHistoryItem,
            is LocalCommandSubmittedChatHistoryItem,
            is ProviderModelChangedChatHistoryItem,
            is StateSnapshotAddedChatHistoryItem,
            is UserSubmittedChatHistoryItem -> Unit
        }
    }
    return states
}
