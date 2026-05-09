package com.strangeparticle.springboard.app.ai.providers.openai

import com.strangeparticle.springboard.app.ai.core.AiErrorClass
import com.strangeparticle.springboard.app.ai.core.AiException
import com.strangeparticle.springboard.app.ai.core.AiResponse
import com.strangeparticle.springboard.app.ai.core.AiStopReason
import com.strangeparticle.springboard.app.ai.core.AiToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses an OpenAI chat-completions response body into the provider-neutral
 * [AiResponse] type. Pure function — no IO. Lives next to [OpenAiRequestBuilder]
 * so request and response handling are testable side by side.
 *
 * Per spec §3.3.
 */
internal object OpenAiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parse a successful (HTTP 200) OpenAI response body into an [AiResponse].
     *
     * The body is `{"choices": [{ "message": { "content": ..., "tool_calls": [...] }, "finish_reason": "..." }], ...}`.
     * Multiple choices are theoretically possible but we only ever request a single
     * choice and consume index 0.
     */
    fun parseSuccess(body: JsonObject): AiResponse {
        val choices = body["choices"] as? JsonArray
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI response has no `choices` array.",
                rawProviderMessage = body.toString(),
            )
        val firstChoice = choices.firstOrNull() as? JsonObject
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI response has empty `choices` array.",
                rawProviderMessage = body.toString(),
            )
        val message = firstChoice["message"] as? JsonObject
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI choice has no `message` object.",
                rawProviderMessage = body.toString(),
            )

        val text = (message["content"] as? JsonPrimitive)?.contentOrNull
        val toolCalls = (message["tool_calls"] as? JsonArray)?.map { entry ->
            val toolCallObject = entry as? JsonObject
                ?: throw AiException(
                    AiErrorClass.MalformedResponse,
                    "OpenAI tool_calls array contains a non-object entry: $entry",
                    rawProviderMessage = entry.toString(),
                )
            parseToolCall(toolCallObject)
        } ?: emptyList()
        val stopReason = mapStopReason((firstChoice["finish_reason"] as? JsonPrimitive)?.contentOrNull)

        return AiResponse(text = text, toolCalls = toolCalls, stopReason = stopReason, raw = body)
    }

    /**
     * Map a non-2xx HTTP response to an [AiException] and throw. [httpStatus] is the
     * HTTP status code; [body] is the raw response body (may be JSON or plain text).
     * Caller should pass null [body] for transport-level failures.
     *
     * Classification consults the body's `error.code` / `error.type` first so
     * provider-specific cases like `context_length_exceeded` and `insufficient_quota`
     * map to the right [AiErrorClass] (the HTTP status alone can't distinguish a
     * quota exhaustion from a transient rate limit on 429, or a context-too-large
     * from any other 400). Falls back to HTTP-status-based classification.
     */
    fun parseErrorAndThrow(httpStatus: Int, body: String?): Nothing {
        val errorEnvelope = body?.let { extractErrorEnvelope(it) }
        val errorClass = errorEnvelope?.let { classifyByErrorEnvelope(it) }
            ?: classifyHttpStatus(httpStatus)
        val rawProviderMessage = errorEnvelope?.message
            ?: body
        throw AiException(
            classified = errorClass,
            message = "OpenAI request failed with HTTP $httpStatus" +
                (rawProviderMessage?.let { ": $it" } ?: ""),
            rawProviderMessage = rawProviderMessage,
        )
    }

    /** Subset of OpenAI's error envelope we actually classify on. */
    private data class ErrorEnvelope(
        val message: String?,
        val type: String?,
        val code: String?,
    )

    private fun extractErrorEnvelope(body: String): ErrorEnvelope? {
        return try {
            val element = json.parseToJsonElement(body) as? JsonObject ?: return null
            val error = element["error"] as? JsonObject ?: return null
            ErrorEnvelope(
                message = (error["message"] as? JsonPrimitive)?.contentOrNull,
                type = (error["type"] as? JsonPrimitive)?.contentOrNull,
                code = (error["code"] as? JsonPrimitive)?.contentOrNull,
            )
        } catch (_: kotlinx.serialization.SerializationException) {
            null
        }
    }

    private fun classifyByErrorEnvelope(envelope: ErrorEnvelope): AiErrorClass? {
        // `code` is the more granular field (e.g. "context_length_exceeded",
        // "insufficient_quota", "rate_limit_exceeded", "invalid_api_key");
        // `type` is the broader category (e.g. "invalid_request_error").
        // Match `code` first.
        when (envelope.code) {
            "context_length_exceeded" -> return AiErrorClass.ContextTooLarge
            "insufficient_quota" -> return AiErrorClass.QuotaExceeded
            "rate_limit_exceeded" -> return AiErrorClass.RateLimit
            "invalid_api_key", "invalid_token" -> return AiErrorClass.InvalidApiKey
        }
        when (envelope.type) {
            "insufficient_quota" -> return AiErrorClass.QuotaExceeded
            "invalid_request_error" -> {
                // Fall through to HTTP-status-based classification — `invalid_request_error`
                // covers many distinct cases (bad input, missing fields, unsupported model)
                // that we don't have specific [AiErrorClass] entries for.
                return null
            }
        }
        return null
    }

    private fun parseToolCall(envelope: JsonObject): AiToolCall {
        val id = (envelope["id"] as? JsonPrimitive)?.contentOrNull
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI tool_call has no `id`.",
                rawProviderMessage = envelope.toString(),
            )
        val function = envelope["function"] as? JsonObject
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI tool_call has no `function` object.",
                rawProviderMessage = envelope.toString(),
            )
        val name = (function["name"] as? JsonPrimitive)?.contentOrNull
            ?: throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI tool_call's function has no `name`.",
                rawProviderMessage = envelope.toString(),
            )
        // OpenAI sends arguments as a JSON-encoded string, even though it's structured.
        val argumentsRaw = (function["arguments"] as? JsonPrimitive)?.contentOrNull ?: "{}"
        val argumentsJson = try {
            json.parseToJsonElement(argumentsRaw) as? JsonObject
                ?: throw AiException(
                    AiErrorClass.MalformedResponse,
                    "OpenAI tool_call arguments are not a JSON object: $argumentsRaw",
                )
        } catch (e: kotlinx.serialization.SerializationException) {
            throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI tool_call arguments are not valid JSON: $argumentsRaw",
                rawProviderMessage = argumentsRaw,
                cause = e,
            )
        }
        return AiToolCall(toolCallId = id, toolName = name, arguments = argumentsJson)
    }

    private fun mapStopReason(finishReason: String?): AiStopReason = when (finishReason) {
        "stop" -> AiStopReason.Stop
        "tool_calls" -> AiStopReason.ToolUse
        "length" -> AiStopReason.MaxTokens
        else -> AiStopReason.Other
    }

    private fun classifyHttpStatus(status: Int): AiErrorClass = when (status) {
        401, 403 -> AiErrorClass.InvalidApiKey
        429 -> AiErrorClass.RateLimit
        in 500..599 -> AiErrorClass.ProviderUnavailable
        else -> AiErrorClass.Unknown
    }

}
