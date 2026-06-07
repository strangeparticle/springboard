package com.strangeparticle.luther.client.provider.openai.response

import com.strangeparticle.luther.client.AiProviderClientErrorType
import com.strangeparticle.luther.client.AiProviderClientException
import com.strangeparticle.luther.client.AiProviderClientResponse
import com.strangeparticle.luther.client.AiProviderClientStopReason
import com.strangeparticle.luther.toolcall.ToolCall
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Parses OpenAI chat-completions DTOs into the provider-neutral [AiProviderClientResponse]
 * type. Pure function — no IO. OpenAiResponseParserTest contains full JSON
 * response and error examples for this deserialization boundary.
 *
 * Per spec §3.3.
 */
internal object OpenAiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parse a successful (HTTP 200) OpenAI response body into an [AiProviderClientResponse].
     *
     * The body is `{"choices": [{ "message": { "content": ..., "tool_calls": [...] }, "finish_reason": "..." }], ...}`.
     * Multiple choices are theoretically possible but we only ever request a single
     * choice and consume index 0.
     */
    fun parseSuccess(body: String): AiProviderClientResponse {
        val raw = parseRawJsonObjectOrThrow(body)
        val response = try {
            // OpenAiResponseParserTest documents the provider JSON structures decoded into this DTO hierarchy.
            json.decodeFromString<com.strangeparticle.luther.client.provider.openai.response.OpenAiChatCompletionResponseDto>(body)
        } catch (e: SerializationException) {
            throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "OpenAI response did not match expected chat-completions shape: ${e.message}",
                rawProviderMessage = body,
                cause = e,
            )
        }
        val firstChoice = response.choices.firstOrNull()
            ?: throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "OpenAI response has empty `choices` array.",
                rawProviderMessage = body,
            )
        val toolCalls = firstChoice.message.toolCalls?.map(::parseToolCall) ?: emptyList()
        return AiProviderClientResponse(
            text = firstChoice.message.content,
            toolCalls = toolCalls,
            stopReason = mapStopReason(firstChoice.finishReason),
            raw = raw,
        )
    }

    /**
     * Map a non-2xx HTTP response to an [AiProviderClientException] and throw. [httpStatus] is the
     * HTTP status code; [body] is the raw response body (may be JSON or plain text).
     * Caller should pass null [body] for transport-level failures.
     *
     * Classification consults the body's `error.code` / `error.type` first so
     * provider-specific cases like `context_length_exceeded` and `insufficient_quota`
     * map to the right [AiProviderClientErrorType] (the HTTP status alone can't distinguish a
     * quota exhaustion from a transient rate limit on 429, or a context-too-large
     * from any other 400). Falls back to HTTP-status-based classification.
     */
    fun parseErrorAndThrow(httpStatus: Int, body: String?): Nothing {
        val openAiError = body?.let { extractOpenAiError(it) }
        val errorClass = openAiError?.let { classifyByOpenAiError(it) }
            ?: classifyHttpStatus(httpStatus)
        val rawProviderMessage = openAiError?.message
            ?: body
        throw AiProviderClientException(
            classified = errorClass,
            message = "OpenAI request failed with HTTP $httpStatus" +
                (rawProviderMessage?.let { ": $it" } ?: ""),
            rawProviderMessage = rawProviderMessage,
        )
    }

    private fun extractOpenAiError(body: String): com.strangeparticle.luther.client.provider.openai.error.OpenAiErrorDto? {
        return try {
            // OpenAiResponseParserTest includes complete error-envelope JSON examples for this DTO.
            json.decodeFromString<com.strangeparticle.luther.client.provider.openai.error.OpenAiErrorResponseDto>(body).error
        } catch (_: SerializationException) {
            null
        }
    }

    private fun classifyByOpenAiError(error: com.strangeparticle.luther.client.provider.openai.error.OpenAiErrorDto): AiProviderClientErrorType? {
        // `code` is the more granular field (e.g. "context_length_exceeded",
        // "insufficient_quota", "rate_limit_exceeded", "invalid_api_key");
        // `type` is the broader category (e.g. "invalid_request_error").
        // Match `code` first.
        when (error.code) {
            "context_length_exceeded" -> return AiProviderClientErrorType.ContextTooLarge
            "insufficient_quota" -> return AiProviderClientErrorType.QuotaExceeded
            "rate_limit_exceeded" -> return AiProviderClientErrorType.RateLimit
            "invalid_api_key", "invalid_token" -> return AiProviderClientErrorType.InvalidApiKey
        }
        when (error.type) {
            "insufficient_quota" -> return AiProviderClientErrorType.QuotaExceeded
            "invalid_request_error" -> {
                // Fall through to HTTP-status-based classification — `invalid_request_error`
                // covers many distinct cases (bad input, missing fields, unsupported model)
                // that we don't have specific [AiErrorClass] entries for.
                return null
            }
        }
        return null
    }

    private fun parseToolCall(toolCall: com.strangeparticle.luther.client.provider.openai.response.OpenAiResponseToolCallDto): ToolCall {
        // OpenAI sends arguments as a JSON-encoded string, even though it's structured.
        val argumentsRaw = toolCall.function.arguments
        try {
            json.parseToJsonElement(argumentsRaw) as? JsonObject
                ?: throw AiProviderClientException(
                    AiProviderClientErrorType.MalformedResponse,
                    "OpenAI tool_call arguments are not a JSON object: $argumentsRaw",
                )
        } catch (e: kotlinx.serialization.SerializationException) {
            throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "OpenAI tool_call arguments are not valid JSON: $argumentsRaw",
                rawProviderMessage = argumentsRaw,
                cause = e,
            )
        }
        return ToolCall(
            toolCallId = toolCall.id,
            toolName = toolCall.function.name,
            argumentsAsJsonString = argumentsRaw,
        )
    }

    private fun parseRawJsonObjectOrThrow(body: String): JsonObject {
        return try {
            json.parseToJsonElement(body) as? JsonObject
                ?: throw AiProviderClientException(
                    AiProviderClientErrorType.MalformedResponse,
                    "OpenAI response was not a JSON object.",
                    rawProviderMessage = body,
                )
        } catch (e: SerializationException) {
            throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "OpenAI response was not valid JSON: ${e.message}",
                rawProviderMessage = body,
                cause = e,
            )
        }
    }

    private fun mapStopReason(finishReason: String?): AiProviderClientStopReason = when (finishReason) {
        "stop" -> AiProviderClientStopReason.Stop
        "tool_calls" -> AiProviderClientStopReason.ToolUse
        "length" -> AiProviderClientStopReason.MaxTokens
        else -> AiProviderClientStopReason.Other
    }

    private fun classifyHttpStatus(status: Int): AiProviderClientErrorType = when (status) {
        401, 403 -> AiProviderClientErrorType.InvalidApiKey
        429 -> AiProviderClientErrorType.RateLimit
        in 500..599 -> AiProviderClientErrorType.ProviderUnavailable
        else -> AiProviderClientErrorType.Unknown
    }

}
