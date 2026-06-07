package com.strangeparticle.luther.session.projection

import com.strangeparticle.luther.session.ChatMessagePart
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
            is ProviderModelChangedChatHistoryItem,
            is StateSnapshotAddedChatHistoryItem -> Unit
        }
    }
    return parts
}
