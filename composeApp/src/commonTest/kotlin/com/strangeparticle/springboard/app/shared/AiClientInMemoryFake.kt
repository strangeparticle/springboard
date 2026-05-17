package com.strangeparticle.springboard.app.shared

import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.client.AiClientResponse
import com.strangeparticle.editio.client.AiClientStopReason
import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.editio.toolcall.ToolCall
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
 *    [AiClientRequest] and returns the [AiClientResponse]. Useful for tests that need to
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
    val recordedRequests: MutableList<AiClientRequest> = mutableListOf()

    /** Calls to [listModels] received in order. */
    val recordedListModelsCalls: MutableList<String> = mutableListOf()

    /** Optional override that decides what to return based on the actual request. */
    var sendAiRequestHandler: ((AiClientRequest) -> AiClientResponse)? = null

    /** Linear queue of responses. `sendAiRequest()` pops the head. */
    val responseQueue: ArrayDeque<AiClientResponse> = ArrayDeque()

    /** What [listModels] returns (for any apiKey). Override per-test. */
    var modelsResponse: List<AiClientModelInfo> = emptyList()

    /** When set, [sendAiRequest] throws this instead of returning a response. */
    var sendAiRequestException: AiClientException? = null

    /** When set, [listModels] throws this instead of returning [modelsResponse]. */
    var listModelsException: AiClientException? = null

    override suspend fun sendAiRequest(request: AiClientRequest): AiClientResponse {
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

    override suspend fun listModels(apiKey: String): List<AiClientModelInfo> {
        recordedListModelsCalls += apiKey
        listModelsException?.let { throw it }
        return modelsResponse
    }

    // -- Convenience builders ----------------------------------------------------

    /** Build a text-only response (model returned prose, no tool calls, finished naturally). */
    fun textOnly(text: String, raw: JsonObject = buildJsonObject {}): AiClientResponse =
        AiClientResponse(
            text = text,
            toolCalls = emptyList(),
            stopReason = AiClientStopReason.Stop,
            raw = raw,
        )

    /** Build a single-tool-call response (model wants the runtime to invoke one tool). */
    fun toolCall(
        toolCallId: String,
        toolName: String,
        arguments: JsonObject,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiClientResponse = AiClientResponse(
        text = text,
        toolCalls = listOf(ToolCall(toolCallId, toolName, arguments.toString())),
        stopReason = AiClientStopReason.ToolUse,
        raw = raw,
    )

    /** Build a multi-tool-call response (model proposes several tool invocations in one turn). */
    fun multipleToolCalls(
        calls: List<ToolCall>,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiClientResponse = AiClientResponse(
        text = text,
        toolCalls = calls,
        stopReason = AiClientStopReason.ToolUse,
        raw = raw,
    )
}
