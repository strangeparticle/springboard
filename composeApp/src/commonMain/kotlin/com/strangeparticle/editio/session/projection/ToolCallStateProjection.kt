package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.editio.session.event.AiChatEvent
import com.strangeparticle.editio.session.event.AssistantErroredAiChatEvent
import com.strangeparticle.editio.session.event.AssistantRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandSubmittedAiChatEvent
import com.strangeparticle.editio.session.event.StateSnapshotAddedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRequestedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRespondedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallCompletedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallDeniedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallFailedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallStartedAiChatEvent
import com.strangeparticle.editio.session.event.UserSubmittedAiChatEvent

internal fun buildToolCallStates(events: List<AiChatEvent>): Map<String, ToolCallState> {
    val states = mutableMapOf<String, ToolCallState>()
    for (event in events) {
        when (event) {
            is ToolCallStartedAiChatEvent -> states[event.toolCall.toolCallId] = ToolCallState.Pending
            is ToolApprovalRequestedAiChatEvent -> states[event.toolCallId] = ToolCallState.ApprovalRequested
            is ToolApprovalRespondedAiChatEvent -> states[event.toolCallId] = ToolCallState.ApprovalResponded(event.approved)
            is ToolCallCompletedAiChatEvent -> states[event.toolCallId] = if (event.endsTurn) {
                ToolCallState.OutputDenied
            } else {
                ToolCallState.OutputAvailable(event.transcriptOutput)
            }
            is ToolCallFailedAiChatEvent -> states[event.toolCallId] = ToolCallState.OutputError(event.message)
            is ToolCallDeniedAiChatEvent -> states[event.toolCallId] = ToolCallState.OutputDenied
            is AssistantErroredAiChatEvent,
            is AssistantRespondedAiChatEvent,
            is LocalCommandRespondedAiChatEvent,
            is LocalCommandSubmittedAiChatEvent,
            is StateSnapshotAddedAiChatEvent,
            is UserSubmittedAiChatEvent -> Unit
        }
    }
    return states
}
