package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.editio.session.event.AssistantErroredAiChatEvent
import com.strangeparticle.editio.session.event.AssistantRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandResponseKind
import com.strangeparticle.editio.session.event.LocalCommandSource
import com.strangeparticle.editio.session.event.LocalCommandSubmittedAiChatEvent
import com.strangeparticle.editio.session.event.StateSnapshotAddedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRequestedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRespondedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallCompletedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallDeniedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallFailedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallStartedAiChatEvent
import com.strangeparticle.editio.session.event.UserSubmittedAiChatEvent
import com.strangeparticle.editio.session.projection.buildProviderHistory
import com.strangeparticle.editio.session.projection.buildToolCallStates
import com.strangeparticle.editio.session.projection.buildTranscriptParts
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.springboard.app.ui.editio.AiChatScrollbackPane
import com.strangeparticle.springboard.app.ui.editio.CommandAttribution
import com.strangeparticle.springboard.app.ui.editio.LocalCommandResponseStyle
import com.strangeparticle.springboard.app.ui.editio.buildDebugScrollbackPanes
import com.strangeparticle.springboard.app.ui.editio.buildSlimScrollbackPanes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class AiChatEventProjectionTest {

    @Test
    fun `local command events project to local command scrollback pane only`() {
        val panes = buildSlimScrollbackPanes(listOf(
            LocalCommandSubmittedAiChatEvent("/help", LocalCommandSource.User),
            LocalCommandRespondedAiChatEvent("/help", "Help text", LocalCommandResponseKind.Help),
        ))

        assertEquals(
            listOf(AiChatScrollbackPane.LocalCommand(
                commandText = "/help",
                commandAttribution = CommandAttribution.User,
                responseText = "Help text",
                style = LocalCommandResponseStyle.Help,
            )),
            panes,
        )
        assertEquals(emptyList(), buildProviderHistory(listOf(
            LocalCommandSubmittedAiChatEvent("/help", LocalCommandSource.User),
            LocalCommandRespondedAiChatEvent("/help", "Help text", LocalCommandResponseKind.Help),
        )))
    }

    @Test
    fun `user and assistant text events project to one interaction pane and provider history`() {
        val events = listOf(
            UserSubmittedAiChatEvent("First message"),
            AssistantRespondedAiChatEvent(text = "First response", toolCalls = emptyList()),
        )

        val interaction = assertIs<AiChatScrollbackPane.Interaction>(buildSlimScrollbackPanes(events).single())
        assertEquals("First message", interaction.requestText)
        assertEquals(listOf(ChatMessagePart.AssistantText("First response")), interaction.responseParts)

        val history = buildProviderHistory(events)
        assertEquals("First message", assertIs<AiClientMessageForUser>(history[0]).text)
        assertEquals("First response", assertIs<AiClientMessageForAssistant>(history[1]).text)
    }

    @Test
    fun `snapshot events project to provider history and debug panes but not slim panes`() {
        val events = listOf(
            StateSnapshotAddedAiChatEvent("{\"tabs\":[]}"),
            UserSubmittedAiChatEvent("What is open?"),
        )

        assertEquals(1, buildSlimScrollbackPanes(events).size)
        assertEquals("{\"tabs\":[]}", assertIs<AiClientMessageForSystemState>(buildProviderHistory(events)[0]).snapshotJson)

        val debug = buildDebugScrollbackPanes(events)
        assertIs<AiChatScrollbackPane.DebugStateSnapshot>(debug[0])
        assertIs<AiChatScrollbackPane.DebugUserMessage>(debug[1])
    }

    @Test
    fun `tool lifecycle events reduce to latest tool call state`() {
        val toolCall = ToolCall("call-1", "record_tool", "{}")
        val events = listOf(
            UserSubmittedAiChatEvent("Run tool"),
            AssistantRespondedAiChatEvent(text = null, toolCalls = listOf(toolCall)),
            ToolCallStartedAiChatEvent(toolCall),
            ToolApprovalRequestedAiChatEvent("call-1"),
            ToolApprovalRespondedAiChatEvent("call-1", approved = true),
            ToolCallCompletedAiChatEvent("call-1", providerContent = "ok", transcriptOutput = "Done", endsTurn = false),
        )

        assertEquals(ToolCallState.OutputAvailable("Done"), buildToolCallStates(events)["call-1"])
        val toolPart = assertIs<ChatMessagePart.ToolCall>(buildTranscriptParts(events).last())
        assertEquals(ToolCallState.OutputAvailable("Done"), toolPart.state)
        val toolResult = assertIs<ToolCallProviderClientMessage>(buildProviderHistory(events).last())
        assertEquals("call-1", toolResult.toolCallId)
        assertEquals("ok", toolResult.content)
    }

    @Test
    fun `tool failure and denial project to terminal transcript states`() {
        val failedTool = ToolCall("call-1", "record_tool", "{}")
        val deniedTool = ToolCall("call-2", "record_tool", "{}")

        val parts = buildTranscriptParts(listOf(
            ToolCallStartedAiChatEvent(failedTool),
            ToolCallFailedAiChatEvent("call-1", providerContent = "failed", message = "Nope"),
            ToolCallStartedAiChatEvent(deniedTool),
            ToolCallDeniedAiChatEvent("call-2"),
        ))

        assertEquals(ToolCallState.OutputError("Nope"), assertIs<ChatMessagePart.ToolCall>(parts[0]).state)
        assertEquals(ToolCallState.OutputDenied, assertIs<ChatMessagePart.ToolCall>(parts[1]).state)
    }

    @Test
    fun `assistant error projects to chat error without provider history entry`() {
        val events = listOf(
            UserSubmittedAiChatEvent("Do it"),
            AssistantErroredAiChatEvent("AI request failed"),
        )

        val interaction = assertIs<AiChatScrollbackPane.Interaction>(buildSlimScrollbackPanes(events).single())
        assertEquals(listOf(ChatMessagePart.ChatError("AI request failed")), interaction.responseParts)
        assertEquals(1, buildProviderHistory(events).size)
    }
}
