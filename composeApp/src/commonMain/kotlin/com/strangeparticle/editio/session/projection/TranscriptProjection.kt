package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.session.ChatMessagePart
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

internal fun buildTranscriptParts(events: List<ChatHistoryItem>): List<ChatMessagePart> {
    val parts = mutableListOf<ChatMessagePart>()
    val toolPartIndices = mutableMapOf<String, Int>()

    fun updateToolPart(toolCallId: String, state: ToolCallState) {
        val index = toolPartIndices[toolCallId] ?: return
        val existing = parts[index] as? ChatMessagePart.ToolCall ?: return
        parts[index] = existing.copy(state = state)
    }

    for (event in events) {
        when (event) {
            is UserSubmittedChatHistoryItem -> parts += ChatMessagePart.UserText(event.text)
            is AssistantRespondedChatHistoryItem -> event.text?.let { parts += ChatMessagePart.AssistantText(it) }
            is AssistantErroredChatHistoryItem -> parts += ChatMessagePart.ChatError(event.message)
            is ToolCallStartedChatHistoryItem -> {
                toolPartIndices[event.toolCall.toolCallId] = parts.size
                parts += ChatMessagePart.ToolCall(event.toolCall, ToolCallState.Pending)
            }
            is ToolApprovalRequestedChatHistoryItem -> updateToolPart(event.toolCallId, ToolCallState.ApprovalRequested)
            is ToolApprovalRespondedChatHistoryItem -> updateToolPart(event.toolCallId, ToolCallState.ApprovalResponded(event.approved))
            is ToolCallCompletedChatHistoryItem -> {
                if (event.endsTurn) {
                    val index = toolPartIndices[event.toolCallId]
                    if (index != null) {
                        parts[index] = ChatMessagePart.AssistantText(event.transcriptOutput)
                    }
                } else {
                    updateToolPart(event.toolCallId, ToolCallState.OutputAvailable(event.transcriptOutput))
                }
            }
            is ToolCallFailedChatHistoryItem -> updateToolPart(event.toolCallId, ToolCallState.OutputError(event.message))
            is ToolCallDeniedChatHistoryItem -> updateToolPart(event.toolCallId, ToolCallState.OutputDenied)
            is LocalCommandRespondedChatHistoryItem,
            is LocalCommandSubmittedChatHistoryItem,
            is StateSnapshotAddedChatHistoryItem -> Unit
        }
    }
    return parts
}
