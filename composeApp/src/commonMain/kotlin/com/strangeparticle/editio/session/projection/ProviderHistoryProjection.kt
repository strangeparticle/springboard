package com.strangeparticle.editio.session.projection

import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
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
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage

internal fun buildProviderHistory(events: List<AiChatEvent>): List<AiClientMessage> = events.mapNotNull { event ->
    when (event) {
        is StateSnapshotAddedAiChatEvent -> AiClientMessageForSystemState(event.snapshotJson)
        is UserSubmittedAiChatEvent -> AiClientMessageForUser(event.text)
        is AssistantRespondedAiChatEvent -> AiClientMessageForAssistant(event.text, event.toolCalls)
        is ToolCallCompletedAiChatEvent -> ToolCallProviderClientMessage(event.toolCallId, event.providerContent)
        is ToolCallFailedAiChatEvent -> ToolCallProviderClientMessage(event.toolCallId, event.providerContent)
        is AssistantErroredAiChatEvent,
        is LocalCommandRespondedAiChatEvent,
        is LocalCommandSubmittedAiChatEvent,
        is ToolApprovalRequestedAiChatEvent,
        is ToolApprovalRespondedAiChatEvent,
        is ToolCallDeniedAiChatEvent,
        is ToolCallStartedAiChatEvent -> null
    }
}
