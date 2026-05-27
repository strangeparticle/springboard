package com.strangeparticle.luther.client.provider.anthropic.response

import com.strangeparticle.luther.client.AiProviderClientErrorType
import com.strangeparticle.luther.client.AiProviderClientException
import com.strangeparticle.luther.client.AiProviderClientResponse
import com.strangeparticle.luther.client.AiProviderClientStopReason
import com.strangeparticle.luther.client.provider.anthropic.error.AnthropicErrorResponseDto
import com.strangeparticle.luther.toolcall.ToolCall
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Parses Anthropic Messages API DTOs into the provider-neutral [AiProviderClientResponse] type.
 * Pure function — no IO. AnthropicResponseParserTest contains full JSON response and
 * error examples for this deserialization boundary.
 */
internal object AnthropicResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseSuccess(body: String): AiProviderClientResponse {
        val raw = parseRawJsonObjectOrThrow(body)
        val response = try {
            json.decodeFromString<AnthropicChatCompletionResponseDto>(body)
        } catch (e: SerializationException) {
            throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
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

        return AiProviderClientResponse(
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
        throw AiProviderClientException(
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

    private fun classifyByAnthropicError(type: String, message: String): AiProviderClientErrorType = when (type) {
        "authentication_error", "permission_error" -> AiProviderClientErrorType.InvalidApiKey
        "rate_limit_error" -> AiProviderClientErrorType.RateLimit
        "api_error", "overloaded_error" -> AiProviderClientErrorType.ProviderUnavailable
        "invalid_request_error" -> {
            val lower = message.lowercase()
            if ("context" in lower || "token" in lower) AiProviderClientErrorType.ContextTooLarge
            else AiProviderClientErrorType.Unknown
        }
        else -> AiProviderClientErrorType.Unknown
    }

    private fun mapStopReason(stopReason: String?): AiProviderClientStopReason = when (stopReason) {
        "end_turn", "stop_sequence" -> AiProviderClientStopReason.Stop
        "tool_use" -> AiProviderClientStopReason.ToolUse
        "max_tokens" -> AiProviderClientStopReason.MaxTokens
        else -> AiProviderClientStopReason.Other
    }

    private fun classifyHttpStatus(status: Int): AiProviderClientErrorType = when (status) {
        401, 403 -> AiProviderClientErrorType.InvalidApiKey
        429 -> AiProviderClientErrorType.RateLimit
        529 -> AiProviderClientErrorType.ProviderUnavailable
        in 500..599 -> AiProviderClientErrorType.ProviderUnavailable
        else -> AiProviderClientErrorType.Unknown
    }

    private fun parseRawJsonObjectOrThrow(body: String): JsonObject = try {
        json.parseToJsonElement(body) as? JsonObject
            ?: throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "Anthropic response was not a JSON object.",
                rawProviderMessage = body,
            )
    } catch (e: SerializationException) {
        throw AiProviderClientException(
            AiProviderClientErrorType.MalformedResponse,
            "Anthropic response was not valid JSON: ${e.message}",
            rawProviderMessage = body,
            cause = e,
        )
    }
}
