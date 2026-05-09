package com.strangeparticle.springboard.app.ai.providers.openai

import com.strangeparticle.springboard.app.ai.core.AiMessage
import com.strangeparticle.springboard.app.ai.core.AiRequest
import com.strangeparticle.springboard.app.ai.core.AiToolCall
import com.strangeparticle.springboard.app.ai.core.AiToolDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Translates a provider-neutral [AiRequest] into the JSON body OpenAI's chat
 * completions API expects. Pure function — no IO, no side effects, fully testable
 * via assertions on the resulting [JsonObject].
 *
 * Wire shape reference: `POST https://api.openai.com/v1/chat/completions` with body
 * `{ "model": ..., "messages": [...], "tools": [...], "tool_choice": "auto" }`.
 *
 * Per spec §3.3.
 */
internal object OpenAiRequestBuilder {

    /**
     * Build the JSON body for a `POST /v1/chat/completions` call.
     *
     * The first message is the system prompt. Subsequent messages come from
     * [AiRequest.history], translated to OpenAI's role-based envelope:
     *
     * - [AiMessage.UserMessage] → `{ "role": "user", "content": ... }`
     * - [AiMessage.AssistantMessage] → `{ "role": "assistant", "content": ..., "tool_calls": [...] }`
     *   (tool_calls present only when the assistant emitted them)
     * - [AiMessage.ToolMessage] → `{ "role": "tool", "tool_call_id": ..., "content": ... }`
     * - [AiMessage.SystemStateMessage] → `{ "role": "user", "content": "<current_state>...</current_state>" }`
     *   (wrapped as a user-role message because OpenAI's chat-completions API only
     *   accepts a single system-role message; using a tagged user message is the
     *   safer convention for the snapshot-injection path)
     */
    fun buildBody(request: AiRequest): JsonObject = buildJsonObject {
        put("model", request.modelId)
        put("messages", buildMessages(request))
        if (request.tools.isNotEmpty()) {
            put("tools", buildTools(request.tools))
            put("tool_choice", "auto")
        }
    }

    private fun buildMessages(request: AiRequest): JsonArray = buildJsonArray {
        addJsonObject {
            put("role", "system")
            put("content", request.systemPrompt)
        }
        for (message in request.history) {
            add(toOpenAiMessage(message))
        }
    }

    private fun toOpenAiMessage(message: AiMessage): JsonObject = when (message) {
        is AiMessage.UserMessage -> buildJsonObject {
            put("role", "user")
            put("content", message.text)
        }
        is AiMessage.AssistantMessage -> buildJsonObject {
            put("role", "assistant")
            // `content` is required (may be null when only tool_calls are present).
            if (message.text != null) put("content", message.text)
            if (message.toolCalls.isNotEmpty()) {
                put("tool_calls", buildJsonArray {
                    for (toolCall in message.toolCalls) add(toOpenAiToolCall(toolCall))
                })
            }
        }
        is AiMessage.ToolMessage -> buildJsonObject {
            put("role", "tool")
            put("tool_call_id", message.toolCallId)
            put("content", message.content)
        }
        is AiMessage.SystemStateMessage -> buildJsonObject {
            put("role", "user")
            put("content", "<current_state>${message.snapshotJson}</current_state>")
        }
    }

    private fun toOpenAiToolCall(toolCall: AiToolCall): JsonObject = buildJsonObject {
        put("id", toolCall.toolCallId)
        put("type", "function")
        put("function", buildJsonObject {
            put("name", toolCall.toolName)
            // OpenAI expects the arguments as a JSON-encoded string, not a nested object.
            put("arguments", toolCall.arguments.toString())
        })
    }

    private fun buildTools(tools: List<AiToolDefinition>): JsonArray = buildJsonArray {
        for (tool in tools) {
            addJsonObject {
                put("type", "function")
                put("function", buildJsonObject {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("parameters", tool.schema)
                })
            }
        }
    }
}
