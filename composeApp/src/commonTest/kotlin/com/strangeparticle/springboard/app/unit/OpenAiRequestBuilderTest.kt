package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.ai.core.AiMessage
import com.strangeparticle.springboard.app.ai.core.AiRequest
import com.strangeparticle.springboard.app.ai.core.AiToolCall
import com.strangeparticle.springboard.app.ai.core.AiToolDefinition
import com.strangeparticle.springboard.app.ai.providers.openai.OpenAiRequestBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [OpenAiRequestBuilder]. Covers the OpenAI chat-completions request
 * envelope: top-level shape, message role mapping for each [AiMessage] variant,
 * and tool-definition adaptation.
 */
internal class OpenAiRequestBuilderTest {

    private fun emptyRequest(
        history: List<AiMessage> = emptyList(),
        tools: List<AiToolDefinition> = emptyList(),
    ) = AiRequest(
        modelId = "gpt-5",
        systemPrompt = "you are an assistant",
        history = history,
        tools = tools,
    )

    @Test
    fun `body has model, messages, and no tools when none provided`() {
        val body = OpenAiRequestBuilder.buildBody(emptyRequest())

        assertEquals("gpt-5", (body["model"] as JsonPrimitive).content)
        assertTrue(body["messages"] is JsonArray)
        assertNull(body["tools"], "tools should be omitted when no tool defs are passed")
        assertNull(body["tool_choice"])
    }

    @Test
    fun `tools and tool_choice are included when at least one tool is passed`() {
        val tool = AiToolDefinition(
            name = "add_app",
            description = "Add an app to a tab.",
            schema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
        )
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(tools = listOf(tool)))

        val tools = body["tools"] as JsonArray
        assertEquals(1, tools.size)

        val function = (tools[0] as JsonObject)["function"] as JsonObject
        assertEquals("add_app", (function["name"] as JsonPrimitive).content)
        assertEquals("Add an app to a tab.", (function["description"] as JsonPrimitive).content)
        assertEquals("auto", (body["tool_choice"] as JsonPrimitive).content)
    }

    @Test
    fun `system prompt is the first message with role=system`() {
        val body = OpenAiRequestBuilder.buildBody(emptyRequest())
        val messages = body["messages"] as JsonArray

        val first = messages[0] as JsonObject
        assertEquals("system", (first["role"] as JsonPrimitive).content)
        assertEquals("you are an assistant", (first["content"] as JsonPrimitive).content)
    }

    @Test
    fun `UserMessage maps to role=user with content`() {
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(
            history = listOf(AiMessage.UserMessage("hello")),
        ))
        val messages = body["messages"] as JsonArray
        val userMsg = messages[1] as JsonObject

        assertEquals("user", (userMsg["role"] as JsonPrimitive).content)
        assertEquals("hello", (userMsg["content"] as JsonPrimitive).content)
    }

    @Test
    fun `AssistantMessage with text only maps to role=assistant with content`() {
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(
            history = listOf(AiMessage.AssistantMessage(text = "ok", toolCalls = emptyList())),
        ))
        val assistantMsg = (body["messages"] as JsonArray)[1] as JsonObject

        assertEquals("assistant", (assistantMsg["role"] as JsonPrimitive).content)
        assertEquals("ok", (assistantMsg["content"] as JsonPrimitive).content)
        assertNull(assistantMsg["tool_calls"])
    }

    @Test
    fun `AssistantMessage with tool_calls maps to assistant with tool_calls array`() {
        val args = buildJsonObject { put("foo", "bar") }
        val toolCall = AiToolCall("call-1", "do_thing", args)
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(
            history = listOf(AiMessage.AssistantMessage(text = null, toolCalls = listOf(toolCall))),
        ))
        val assistantMsg = (body["messages"] as JsonArray)[1] as JsonObject

        assertEquals("assistant", (assistantMsg["role"] as JsonPrimitive).content)
        val toolCalls = assistantMsg["tool_calls"] as JsonArray
        assertEquals(1, toolCalls.size)

        val first = toolCalls[0] as JsonObject
        assertEquals("call-1", (first["id"] as JsonPrimitive).content)
        assertEquals("function", (first["type"] as JsonPrimitive).content)
        val function = first["function"] as JsonObject
        assertEquals("do_thing", (function["name"] as JsonPrimitive).content)
        // arguments are serialized as a JSON string per OpenAI's spec.
        val argsString = (function["arguments"] as JsonPrimitive).content
        assertTrue(argsString.contains("\"foo\""))
        assertTrue(argsString.contains("\"bar\""))
    }

    @Test
    fun `ToolMessage maps to role=tool with tool_call_id and content`() {
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(
            history = listOf(AiMessage.ToolMessage("call-1", """{"ok":true}""")),
        ))
        val toolMsg = (body["messages"] as JsonArray)[1] as JsonObject

        assertEquals("tool", (toolMsg["role"] as JsonPrimitive).content)
        assertEquals("call-1", (toolMsg["tool_call_id"] as JsonPrimitive).content)
        assertEquals("""{"ok":true}""", (toolMsg["content"] as JsonPrimitive).content)
    }

    @Test
    fun `SystemStateMessage maps to a user-role message wrapped in current_state tags`() {
        val snapshot = """{"tabs":[],"activeTabId":null}"""
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(
            history = listOf(AiMessage.SystemStateMessage(snapshot)),
        ))
        val stateMsg = (body["messages"] as JsonArray)[1] as JsonObject

        assertEquals("user", (stateMsg["role"] as JsonPrimitive).content)
        val content = (stateMsg["content"] as JsonPrimitive).content
        assertTrue(content.startsWith("<current_state>"))
        assertTrue(content.endsWith("</current_state>"))
        assertTrue(content.contains(snapshot))
    }

    @Test
    fun `tool definitions wrap their JSON schema unchanged as the function parameters`() {
        val schema = buildJsonObject {
            put("type", "object")
            put("required", kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("tab_id"))))
        }
        val tool = AiToolDefinition(name = "save_springboard", description = "Save.", schema = schema)
        val body = OpenAiRequestBuilder.buildBody(emptyRequest(tools = listOf(tool)))
        val function = ((body["tools"] as JsonArray)[0] as JsonObject)["function"] as JsonObject

        // The schema passes through verbatim.
        assertEquals(schema, function["parameters"])
    }
}
