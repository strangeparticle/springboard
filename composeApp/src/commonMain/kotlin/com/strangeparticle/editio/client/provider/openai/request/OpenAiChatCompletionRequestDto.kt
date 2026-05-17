package com.strangeparticle.editio.client.provider.openai.request

import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.toolcall.AiToolCallDefinition
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class OpenAiChatCompletionRequestDto(
    val model: String,
    val messages: List<com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto>,
    val tools: List<com.strangeparticle.editio.client.provider.openai.request.OpenAiToolDto>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
) {
    companion object {
        fun from(request: AiClientRequest): OpenAiChatCompletionRequestDto = OpenAiChatCompletionRequestDto(
            model = request.modelId,
            messages = buildMessages(request),
            tools = request.tools.takeIf { it.isNotEmpty() }?.map(::toOpenAiTool),
            toolChoice = request.tools.takeIf { it.isNotEmpty() }?.let { "auto" },
            maxTokens = request.maxTokens,
        )

        private fun buildMessages(request: AiClientRequest): List<com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto> = buildList {
            add(
                _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto(
                    role = "system",
                    content = request.systemPrompt.toJsonElement()
                )
            )
            addAll(request.history.map(::toOpenAiMessage))
        }

        private fun toOpenAiMessage(message: AiClientMessage): com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto = when (message) {
            is AiClientMessageForUser -> _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto(
                role = "user",
                content = message.text.toJsonElement(),
            )
            is AiClientMessageForAssistant -> _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto(
                role = "assistant",
                content = message.text?.toJsonElement() ?: JsonNull,
                toolCalls = message.toolCalls.takeIf { it.isNotEmpty() }?.map(::toOpenAiToolCall),
            )
            is ToolCallProviderClientMessage -> _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto(
                role = "tool",
                toolCallId = message.toolCallId,
                content = message.content.toJsonElement(),
            )
            is AiClientMessageForSystemState -> _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiMessageDto(
                role = "user",
                content = "<current_state>${message.snapshotJson}</current_state>".toJsonElement(),
            )
            else -> error("Unsupported OpenAI provider message type: ${message::class.simpleName}")
        }

        private fun toOpenAiToolCall(toolCall: ToolCall): com.strangeparticle.editio.client.provider.openai.request.OpenAiToolCallDto =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiToolCallDto(
                id = toolCall.toolCallId,
                type = "function",
                function = _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiToolCallFunctionDto(
                    name = toolCall.toolName,
                    arguments = toolCall.argumentsAsJsonString,
                ),
            )

        private fun toOpenAiTool(tool: AiToolCallDefinition): com.strangeparticle.editio.client.provider.openai.request.OpenAiToolDto =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiToolDto(
                type = "function",
                function = _root_ide_package_.com.strangeparticle.editio.client.provider.openai.request.OpenAiToolFunctionDto(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.schema,
                ),
            )

        private fun String.toJsonElement(): JsonElement = JsonPrimitive(this)
    }
}
