package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.session.ToolCallState
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
            is StateSnapshotAddedChatHistoryItem,
            is UserSubmittedChatHistoryItem -> Unit
        }
    }
    return states
}
