package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.session.ChatMessagePart
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

internal fun buildTranscriptParts(events: List<AiChatEvent>): List<ChatMessagePart> {
    val parts = mutableListOf<ChatMessagePart>()
    val toolPartIndices = mutableMapOf<String, Int>()

    fun updateToolPart(toolCallId: String, state: ToolCallState) {
        val index = toolPartIndices[toolCallId] ?: return
        val existing = parts[index] as? ChatMessagePart.ToolCall ?: return
        parts[index] = existing.copy(state = state)
    }

    for (event in events) {
        when (event) {
            is UserSubmittedAiChatEvent -> parts += ChatMessagePart.UserText(event.text)
            is AssistantRespondedAiChatEvent -> event.text?.let { parts += ChatMessagePart.AssistantText(it) }
            is AssistantErroredAiChatEvent -> parts += ChatMessagePart.ChatError(event.message)
            is ToolCallStartedAiChatEvent -> {
                toolPartIndices[event.toolCall.toolCallId] = parts.size
                parts += ChatMessagePart.ToolCall(event.toolCall, ToolCallState.Pending)
            }
            is ToolApprovalRequestedAiChatEvent -> updateToolPart(event.toolCallId, ToolCallState.ApprovalRequested)
            is ToolApprovalRespondedAiChatEvent -> updateToolPart(event.toolCallId, ToolCallState.ApprovalResponded(event.approved))
            is ToolCallCompletedAiChatEvent -> {
                if (event.endsTurn) {
                    val index = toolPartIndices[event.toolCallId]
                    if (index != null) {
                        parts[index] = ChatMessagePart.AssistantText(event.transcriptOutput)
                    }
                } else {
                    updateToolPart(event.toolCallId, ToolCallState.OutputAvailable(event.transcriptOutput))
                }
            }
            is ToolCallFailedAiChatEvent -> updateToolPart(event.toolCallId, ToolCallState.OutputError(event.message))
            is ToolCallDeniedAiChatEvent -> updateToolPart(event.toolCallId, ToolCallState.OutputDenied)
            is LocalCommandRespondedAiChatEvent,
            is LocalCommandSubmittedAiChatEvent,
            is StateSnapshotAddedAiChatEvent -> Unit
        }
    }
    return parts
}
