package com.strangeparticle.springboard.app.unit

import com.strangeparticle.luther.conversation.AiConversationMessageForAssistant
import com.strangeparticle.luther.conversation.AiConversationMessageForSystemState
import com.strangeparticle.luther.conversation.AiConversationMessageForUser
import com.strangeparticle.luther.session.ChatHistoryGroup
import com.strangeparticle.luther.session.ChatHistoryGroupType
import com.strangeparticle.luther.session.ChatMessagePart
import com.strangeparticle.luther.session.ToolCallState
import com.strangeparticle.luther.session.buildChatHistoryDebugDumpJson
import com.strangeparticle.luther.session.event.AssistantErroredChatHistoryItem
import com.strangeparticle.luther.session.event.AssistantRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandResponseKind
import com.strangeparticle.luther.session.event.LocalCommandSource
import com.strangeparticle.luther.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.luther.session.event.StateSnapshotAddedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRequestedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolApprovalRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallCompletedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallDeniedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallFailedChatHistoryItem
import com.strangeparticle.luther.session.event.ToolCallStartedChatHistoryItem
import com.strangeparticle.luther.session.event.UserSubmittedChatHistoryItem
import com.strangeparticle.luther.session.projection.buildProviderHistory
import com.strangeparticle.luther.session.projection.buildToolCallStates
import com.strangeparticle.luther.session.projection.buildTranscriptParts
import com.strangeparticle.luther.toolcall.ToolCall
import com.strangeparticle.luther.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.springboard.app.ui.luther.AiChatScrollbackPane
import com.strangeparticle.springboard.app.ui.luther.CommandAttribution
import com.strangeparticle.springboard.app.ui.luther.LocalCommandResponseStyle
import com.strangeparticle.springboard.app.ui.luther.buildDebugScrollbackPanes
import com.strangeparticle.springboard.app.ui.luther.buildSlimScrollbackPanes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

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

    @Test
    fun `debug dump serializes provider model system prompt and raw grouped history`() {
        val groups = listOf(
            ChatHistoryGroup(ChatHistoryGroupType.LOCAL_COMMAND, listOf(
                LocalCommandSubmittedChatHistoryItem("/help", LocalCommandSource.User),
                LocalCommandRespondedChatHistoryItem("/help", "Help text", LocalCommandResponseKind.Help),
            )),
            ChatHistoryGroup(ChatHistoryGroupType.AI_INTERACTION, listOf(
                StateSnapshotAddedChatHistoryItem("{\"tabs\":[]}"),
                UserSubmittedChatHistoryItem("Run tool"),
                AssistantRespondedChatHistoryItem(
                    text = "Calling a tool",
                    toolCalls = listOf(ToolCall("call-1", "record_tool", "{\"value\":\"quoted\\nline\"}")),
                ),
                ToolApprovalRequestedChatHistoryItem("call-1"),
                ToolApprovalRespondedChatHistoryItem("call-1", approved = true),
                ToolCallStartedChatHistoryItem(ToolCall("call-1", "record_tool", "{\"value\":\"quoted\\nline\"}")),
                ToolCallCompletedChatHistoryItem("call-1", providerContent = "{\"ok\":true}", transcriptOutput = "Done", endsTurn = false),
                ToolCallFailedChatHistoryItem("call-2", providerContent = "{\"ok\":false}", message = "Failed"),
                ToolCallDeniedChatHistoryItem("call-3"),
                AssistantErroredChatHistoryItem("AI request failed"),
            )),
        )

        val dumpJson = buildChatHistoryDebugDumpJson(
            groups = groups,
            providerLabel = "OpenAI",
            modelLabel = "gpt-5",
            systemPrompt = "system prompt",
        )

        val root = Json.parseToJsonElement(dumpJson).jsonObject
        assertEquals(JsonPrimitive("SpringboardAiDebugDump"), root["kind"])
        assertEquals(JsonPrimitive("OpenAI"), root["provider"])
        assertEquals(JsonPrimitive("gpt-5"), root["model"])
        assertEquals(JsonPrimitive("system prompt"), root["systemPrompt"])
        assertEquals(JsonPrimitive(2), root["groupCount"])
        assertEquals(JsonPrimitive(12), root["itemCount"])

        val groupsJson = root["groups"] as JsonArray
        val localGroup = groupsJson[0] as JsonObject
        assertEquals(JsonPrimitive("LOCAL_COMMAND"), localGroup["type"])
        assertEquals(JsonPrimitive(0), localGroup["groupIndex"])

        val aiItems = ((groupsJson[1] as JsonObject)["items"] as JsonArray)
        assertEquals(JsonPrimitive("StateSnapshotAddedChatHistoryItem"), (aiItems[0] as JsonObject)["kind"])
        assertEquals(JsonPrimitive("{\"tabs\":[]}"), (aiItems[0] as JsonObject)["snapshotJson"])
        assertEquals(JsonPrimitive("AssistantRespondedChatHistoryItem"), (aiItems[2] as JsonObject)["kind"])

        val toolCalls = (aiItems[2] as JsonObject)["toolCalls"] as JsonArray
        assertEquals(JsonPrimitive("call-1"), (toolCalls[0] as JsonObject)["toolCallId"])
        assertEquals(JsonPrimitive("{\"value\":\"quoted\\nline\"}"), (toolCalls[0] as JsonObject)["argumentsAsJsonString"])
        assertEquals(JsonPrimitive("ToolCallFailedChatHistoryItem"), (aiItems[7] as JsonObject)["kind"])
        assertEquals(JsonPrimitive("{\"ok\":false}"), (aiItems[7] as JsonObject)["providerContent"])
        assertEquals(JsonPrimitive("ToolCallDeniedChatHistoryItem"), (aiItems[8] as JsonObject)["kind"])
        assertEquals(JsonPrimitive("AssistantErroredChatHistoryItem"), (aiItems[9] as JsonObject)["kind"])
    }
}
