package com.strangeparticle.editio.client.provider.anthropic.request

import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.toolcall.AiToolCallDefinition
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Top-level Anthropic Messages API request DTO.
 *
 * AnthropicChatCompletionRequestTest documents the full serialized JSON shapes produced by [from].
 */
@Serializable
internal data class AnthropicChatCompletionRequestDto(
    val model: String,
    val messages: List<AnthropicMessageDto>,
    val system: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val tools: List<AnthropicToolDto>? = null,
    @SerialName("tool_choice") val toolChoice: AnthropicToolChoiceDto? = null,
) {
    companion object {

        private const val DEFAULT_MAX_TOKENS = 8192

        fun from(request: AiClientRequest): AnthropicChatCompletionRequestDto =
            AnthropicChatCompletionRequestDto(
                model = request.modelId,
                messages = buildMessages(request.history),
                system = request.systemPrompt,
                maxTokens = request.maxTokens ?: DEFAULT_MAX_TOKENS,
                tools = request.tools.takeIf { it.isNotEmpty() }?.map(::toAnthropicTool),
                toolChoice = request.tools.takeIf { it.isNotEmpty() }?.let { AnthropicToolChoiceDto("auto") },
            )

        /**
         * Map history to Anthropic messages, merging consecutive user-role turns into one.
         *
         * Anthropic requires strictly alternating user/assistant roles. Two cases produce
         * back-to-back user turns that must be merged:
         *  1. AiClientMessageForSystemState followed by AiClientMessageForUser
         *  2. Multiple consecutive ToolCallProviderClientMessage instances (all → user role)
         *
         * Consecutive user-role inputs accumulate into a pending list and are flushed as
         * a single AnthropicMessageDto with a JsonArray content when an assistant turn arrives
         * or the history ends.
         */
        private fun buildMessages(history: List<AiClientMessage>): List<AnthropicMessageDto> {
            val result = mutableListOf<AnthropicMessageDto>()
            val pendingUserBlocks = mutableListOf<JsonElement>()

            fun flushPendingUser() {
                if (pendingUserBlocks.isEmpty()) return
                val content: JsonElement = when {
                    pendingUserBlocks.size == 1 && (pendingUserBlocks[0] as? JsonObject)
                        ?.get("type")?.let { (it as? JsonPrimitive)?.content } == "text" -> {
                        // Single text block — Anthropic accepts a plain string, which is simpler
                        JsonPrimitive((pendingUserBlocks[0] as JsonObject)["text"]!!.let { (it as JsonPrimitive).content })
                    }
                    else -> JsonArray(pendingUserBlocks.toList())
                }
                result.add(AnthropicMessageDto(role = "user", content = content))
                pendingUserBlocks.clear()
            }

            for (message in history) {
                when (message) {
                    is AiClientMessageForUser -> {
                        pendingUserBlocks.add(textBlock(message.text))
                    }
                    is AiClientMessageForSystemState -> {
                        pendingUserBlocks.add(textBlock("<current_state>${message.snapshotJson}</current_state>"))
                    }
                    is ToolCallProviderClientMessage -> {
                        pendingUserBlocks.add(toolResultBlock(message.toolCallId, message.content))
                    }
                    is AiClientMessageForAssistant -> {
                        flushPendingUser()
                        result.add(toAssistantMessage(message))
                    }
                    else -> error("Unsupported Anthropic provider message type: ${message::class.simpleName}")
                }
            }
            flushPendingUser()
            return result
        }

        private fun toAssistantMessage(message: AiClientMessageForAssistant): AnthropicMessageDto {
            val blocks = buildList {
                message.text?.let { add(textBlock(it)) }
                message.toolCalls.forEach { add(toolUseBlock(it)) }
            }
            val content: JsonElement = if (blocks.size == 1 && message.toolCalls.isEmpty()) {
                // Text-only assistant turn — plain string is accepted and preferred
                JsonPrimitive(message.text!!)
            } else {
                JsonArray(blocks)
            }
            return AnthropicMessageDto(role = "assistant", content = content)
        }

        private fun textBlock(text: String): JsonObject = buildJsonObject {
            put("type", "text")
            put("text", text)
        }

        private fun toolUseBlock(toolCall: ToolCall): JsonObject = buildJsonObject {
            put("type", "tool_use")
            put("id", toolCall.toolCallId)
            put("name", toolCall.toolName)
            put("input", Json.parseToJsonElement(toolCall.argumentsAsJsonString))
        }

        private fun toolResultBlock(toolCallId: String, content: String): JsonObject = buildJsonObject {
            put("type", "tool_result")
            put("tool_use_id", toolCallId)
            put("content", content)
        }

        private fun toAnthropicTool(tool: AiToolCallDefinition): AnthropicToolDto =
            AnthropicToolDto(
                name = tool.name,
                description = tool.description,
                inputSchema = tool.schema,
            )
    }
}
