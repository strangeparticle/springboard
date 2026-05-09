package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.ai.core.AiClient
import com.strangeparticle.springboard.app.ai.core.AiException
import com.strangeparticle.springboard.app.ai.core.AiModelInfo
import com.strangeparticle.springboard.app.ai.core.AiRequest
import com.strangeparticle.springboard.app.ai.core.AiResponse
import com.strangeparticle.springboard.app.ai.core.AiStopReason
import com.strangeparticle.springboard.app.ai.core.AiToolCall
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Test double for [AiClient]. Tests script the responses they want; the fake records
 * everything that flows through it so assertions can verify request shape without
 * touching Ktor or any HTTP transport.
 *
 * Two ways to script behavior, in priority order:
 *
 * 1. **Per-request handler.** Set [sendAiRequestHandler] to a function that takes the
 *    [AiRequest] and returns the [AiResponse]. Useful for tests that need to
 *    inspect the request to decide what to send back (e.g. agent-loop iterations
 *    where the second request looks different from the first).
 * 2. **Response queue.** Push responses to [responseQueue] in order; each
 *    `sendAiRequest()` pops the head. Useful for simple linear scenarios.
 *
 * If both are unset and `sendAiRequest()` is called, the fake throws to make the missing
 * setup obvious in test output.
 *
 * Per spec §7.4.
 */
internal class AiClientInMemoryFake : AiClient {

    /** Calls received in order. Inspect after the test to assert request shape. */
    val recordedRequests: MutableList<AiRequest> = mutableListOf()

    /** Calls to [listModels] received in order. */
    val recordedListModelsCalls: MutableList<String> = mutableListOf()

    /** Optional override that decides what to return based on the actual request. */
    var sendAiRequestHandler: ((AiRequest) -> AiResponse)? = null

    /** Linear queue of responses. `sendAiRequest()` pops the head. */
    val responseQueue: ArrayDeque<AiResponse> = ArrayDeque()

    /** What [listModels] returns (for any apiKey). Override per-test. */
    var modelsResponse: List<AiModelInfo> = emptyList()

    /** When set, [sendAiRequest] throws this instead of returning a response. */
    var sendAiRequestException: AiException? = null

    /** When set, [listModels] throws this instead of returning [modelsResponse]. */
    var listModelsException: AiException? = null

    override suspend fun sendAiRequest(request: AiRequest): AiResponse {
        recordedRequests += request
        sendAiRequestException?.let { throw it }
        sendAiRequestHandler?.let { return it(request) }
        if (responseQueue.isEmpty()) {
            throw IllegalStateException(
                "AiClientInMemoryFake.sendAiRequest() called but neither sendAiRequestHandler " +
                    "nor responseQueue was configured. Test setup error."
            )
        }
        return responseQueue.removeFirst()
    }

    override suspend fun listModels(apiKey: String): List<AiModelInfo> {
        recordedListModelsCalls += apiKey
        listModelsException?.let { throw it }
        return modelsResponse
    }

    // -- Convenience builders ----------------------------------------------------

    /** Build a text-only response (model returned prose, no tool calls, finished naturally). */
    fun textOnly(text: String, raw: JsonObject = buildJsonObject {}): AiResponse =
        AiResponse(
            text = text,
            toolCalls = emptyList(),
            stopReason = AiStopReason.Stop,
            raw = raw,
        )

    /** Build a single-tool-call response (model wants the runtime to invoke one tool). */
    fun toolCall(
        toolCallId: String,
        toolName: String,
        arguments: JsonObject,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiResponse = AiResponse(
        text = text,
        toolCalls = listOf(AiToolCall(toolCallId, toolName, arguments)),
        stopReason = AiStopReason.ToolUse,
        raw = raw,
    )

    /** Build a multi-tool-call response (model proposes several tool invocations in one turn). */
    fun multipleToolCalls(
        calls: List<AiToolCall>,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiResponse = AiResponse(
        text = text,
        toolCalls = calls,
        stopReason = AiStopReason.ToolUse,
        raw = raw,
    )
}
