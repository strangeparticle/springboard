package com.strangeparticle.editio.client.provider.anthropic.response

import com.strangeparticle.editio.client.AiClientErrorType
import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.client.AiClientResponse
import com.strangeparticle.editio.client.AiClientStopReason
import com.strangeparticle.editio.client.provider.anthropic.error.AnthropicErrorResponseDto
import com.strangeparticle.editio.toolcall.ToolCall
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Parses Anthropic Messages API DTOs into the provider-neutral [AiClientResponse] type.
 * Pure function — no IO. AnthropicResponseParserTest contains full JSON response and
 * error examples for this deserialization boundary.
 */
internal object AnthropicResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseSuccess(body: String): AiClientResponse {
        val raw = parseRawJsonObjectOrThrow(body)
        val response = try {
            json.decodeFromString<AnthropicChatCompletionResponseDto>(body)
        } catch (e: SerializationException) {
            throw AiClientException(
                AiClientErrorType.MalformedResponse,
                "Anthropic response did not match expected messages shape: ${e.message}",
                rawProviderMessage = body,
                cause = e,
            )
        }

        val text = response.content
            .filterIsInstance<AnthropicResponseContentBlockDto.Text>()
            .joinToString("") { it.text }
            .takeIf { it.isNotEmpty() }

        val toolCalls = response.content
            .filterIsInstance<AnthropicResponseContentBlockDto.ToolUse>()
            .map { block ->
                ToolCall(
                    toolCallId = block.id,
                    toolName = block.name,
                    argumentsAsJsonString = json.encodeToString(JsonObject.serializer(), block.input),
                )
            }

        return AiClientResponse(
            text = text,
            toolCalls = toolCalls,
            stopReason = mapStopReason(response.stopReason),
            raw = raw,
        )
    }

    fun parseErrorAndThrow(httpStatus: Int, body: String?): Nothing {
        val anthropicError = body?.let { extractAnthropicError(it) }
        val errorType = anthropicError?.let { classifyByAnthropicError(it.type, it.message) }
            ?: classifyHttpStatus(httpStatus)
        val rawProviderMessage = anthropicError?.message ?: body
        throw AiClientException(
            classified = errorType,
            message = "Anthropic request failed with HTTP $httpStatus" +
                (rawProviderMessage?.let { ": $it" } ?: ""),
            rawProviderMessage = rawProviderMessage,
        )
    }

    fun parseModelListResponse(body: String): JsonObject = parseRawJsonObjectOrThrow(body)

    private fun extractAnthropicError(body: String) = try {
        json.decodeFromString<AnthropicErrorResponseDto>(body).error
    } catch (_: SerializationException) {
        null
    }

    private fun classifyByAnthropicError(type: String, message: String): AiClientErrorType = when (type) {
        "authentication_error", "permission_error" -> AiClientErrorType.InvalidApiKey
        "rate_limit_error" -> AiClientErrorType.RateLimit
        "api_error", "overloaded_error" -> AiClientErrorType.ProviderUnavailable
        "invalid_request_error" -> {
            val lower = message.lowercase()
            if ("context" in lower || "token" in lower) AiClientErrorType.ContextTooLarge
            else AiClientErrorType.Unknown
        }
        else -> AiClientErrorType.Unknown
    }

    private fun mapStopReason(stopReason: String?): AiClientStopReason = when (stopReason) {
        "end_turn", "stop_sequence" -> AiClientStopReason.Stop
        "tool_use" -> AiClientStopReason.ToolUse
        "max_tokens" -> AiClientStopReason.MaxTokens
        else -> AiClientStopReason.Other
    }

    private fun classifyHttpStatus(status: Int): AiClientErrorType = when (status) {
        401, 403 -> AiClientErrorType.InvalidApiKey
        429 -> AiClientErrorType.RateLimit
        529 -> AiClientErrorType.ProviderUnavailable
        in 500..599 -> AiClientErrorType.ProviderUnavailable
        else -> AiClientErrorType.Unknown
    }

    private fun parseRawJsonObjectOrThrow(body: String): JsonObject = try {
        json.parseToJsonElement(body) as? JsonObject
            ?: throw AiClientException(
                AiClientErrorType.MalformedResponse,
                "Anthropic response was not a JSON object.",
                rawProviderMessage = body,
            )
    } catch (e: SerializationException) {
        throw AiClientException(
            AiClientErrorType.MalformedResponse,
            "Anthropic response was not valid JSON: ${e.message}",
            rawProviderMessage = body,
            cause = e,
        )
    }
}
