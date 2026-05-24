package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.conversation.AiConversationMessageForAssistant
import com.strangeparticle.editio.conversation.AiConversationMessageForSystemState
import com.strangeparticle.editio.conversation.AiConversationMessageForUser
import com.strangeparticle.editio.session.ChatHistoryGroup
import com.strangeparticle.editio.session.ChatHistoryGroupType
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.editio.session.event.AssistantErroredChatHistoryItem
import com.strangeparticle.editio.session.event.AssistantRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.LocalCommandResponseKind
import com.strangeparticle.editio.session.event.LocalCommandSource
import com.strangeparticle.editio.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.editio.session.event.StateSnapshotAddedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolApprovalRequestedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolApprovalRespondedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallCompletedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallDeniedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallFailedChatHistoryItem
import com.strangeparticle.editio.session.event.ToolCallStartedChatHistoryItem
import com.strangeparticle.editio.session.event.UserSubmittedChatHistoryItem
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

internal class ChatHistoryProjectionTest {

    @Test
    fun `local command group projects to local command scrollback pane only`() {
        val groups = listOf(
            ChatHistoryGroup(ChatHistoryGroupType.LOCAL_COMMAND, listOf(
                LocalCommandSubmittedChatHistoryItem("/help", LocalCommandSource.User),
                LocalCommandRespondedChatHistoryItem("/help", "Help text", LocalCommandResponseKind.Help),
            )),
        )

        assertEquals(
            listOf(AiChatScrollbackPane.LocalCommand(
                commandText = "/help",
                commandAttribution = CommandAttribution.User,
                responseText = "Help text",
                style = LocalCommandResponseStyle.Help,
            )),
            buildSlimScrollbackPanes(groups),
        )
        assertEquals(emptyList(), buildProviderHistory(listOf(
            LocalCommandSubmittedChatHistoryItem("/help", LocalCommandSource.User),
            LocalCommandRespondedChatHistoryItem("/help", "Help text", LocalCommandResponseKind.Help),
        )))
    }

    @Test
    fun `ai interaction group projects to one interaction pane and provider history`() {
        val items = listOf(
            UserSubmittedChatHistoryItem("First message"),
            AssistantRespondedChatHistoryItem(text = "First response", toolCalls = emptyList()),
        )
        val groups = listOf(ChatHistoryGroup(ChatHistoryGroupType.AI_INTERACTION, items))

        val interaction = assertIs<AiChatScrollbackPane.Interaction>(buildSlimScrollbackPanes(groups).single())
        assertEquals("First message", interaction.requestText)
        assertEquals(listOf(ChatMessagePart.AssistantText("First response")), interaction.responseParts)

        val history = buildProviderHistory(items)
        assertEquals("First message", assertIs<AiConversationMessageForUser>(history[0]).text)
        assertEquals("First response", assertIs<AiConversationMessageForAssistant>(history[1]).text)
    }

    @Test
    fun `snapshot items project to provider history and debug panes but not slim panes`() {
        val items = listOf(
            StateSnapshotAddedChatHistoryItem("{\"tabs\":[]}"),
            UserSubmittedChatHistoryItem("What is open?"),
        )
        val groups = listOf(ChatHistoryGroup(ChatHistoryGroupType.AI_INTERACTION, items))

        assertEquals(1, buildSlimScrollbackPanes(groups).size)
        assertEquals("{\"tabs\":[]}", assertIs<AiConversationMessageForSystemState>(buildProviderHistory(items)[0]).snapshotJson)

        val debug = buildDebugScrollbackPanes(groups)
        assertIs<AiChatScrollbackPane.DebugStateSnapshot>(debug[0])
        assertIs<AiChatScrollbackPane.DebugUserMessage>(debug[1])
    }

    @Test
    fun `tool lifecycle items reduce to latest tool call state`() {
        val toolCall = ToolCall("call-1", "record_tool", "{}")
        val items = listOf(
            UserSubmittedChatHistoryItem("Run tool"),
            AssistantRespondedChatHistoryItem(text = null, toolCalls = listOf(toolCall)),
            ToolCallStartedChatHistoryItem(toolCall),
            ToolApprovalRequestedChatHistoryItem("call-1"),
            ToolApprovalRespondedChatHistoryItem("call-1", approved = true),
            ToolCallCompletedChatHistoryItem("call-1", providerContent = "ok", transcriptOutput = "Done", endsTurn = false),
        )

        assertEquals(ToolCallState.OutputAvailable("Done"), buildToolCallStates(items)["call-1"])
        val toolPart = assertIs<ChatMessagePart.ToolCall>(buildTranscriptParts(items).last())
        assertEquals(ToolCallState.OutputAvailable("Done"), toolPart.state)
        val toolResult = assertIs<ToolCallProviderClientMessage>(buildProviderHistory(items).last())
        assertEquals("call-1", toolResult.toolCallId)
        assertEquals("ok", toolResult.content)
    }

    @Test
    fun `tool failure and denial project to terminal transcript states`() {
        val failedTool = ToolCall("call-1", "record_tool", "{}")
        val deniedTool = ToolCall("call-2", "record_tool", "{}")

        val parts = buildTranscriptParts(listOf(
            ToolCallStartedChatHistoryItem(failedTool),
            ToolCallFailedChatHistoryItem("call-1", providerContent = "failed", message = "Nope"),
            ToolCallStartedChatHistoryItem(deniedTool),
            ToolCallDeniedChatHistoryItem("call-2"),
        ))

        assertEquals(ToolCallState.OutputError("Nope"), assertIs<ChatMessagePart.ToolCall>(parts[0]).state)
        assertEquals(ToolCallState.OutputDenied, assertIs<ChatMessagePart.ToolCall>(parts[1]).state)
    }

    @Test
    fun `assistant error projects to chat error without provider history entry`() {
        val items = listOf(
            UserSubmittedChatHistoryItem("Do it"),
            AssistantErroredChatHistoryItem("AI request failed"),
        )
        val groups = listOf(ChatHistoryGroup(ChatHistoryGroupType.AI_INTERACTION, items))

        val interaction = assertIs<AiChatScrollbackPane.Interaction>(buildSlimScrollbackPanes(groups).single())
        assertEquals(listOf(ChatMessagePart.ChatError("AI request failed")), interaction.responseParts)
        assertEquals(1, buildProviderHistory(items).size)
    }
}
