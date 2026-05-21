package com.strangeparticle.springboard.app.shared

import com.strangeparticle.editio.client.AiProviderClientException
import com.strangeparticle.editio.client.AiProviderClientRequest
import com.strangeparticle.editio.client.AiProviderClientResponse
import com.strangeparticle.editio.client.AiProviderClientStopReason
import com.strangeparticle.editio.client.AiProviderClient
import com.strangeparticle.editio.client.AiProviderClientModelInfo
import com.strangeparticle.editio.toolcall.ToolCall
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Test double for [AiProviderClient]. Tests script the responses they want; the fake records
 * everything that flows through it so assertions can verify request shape without
 * touching Ktor or any HTTP transport.
 *
 * Two ways to script behavior, in priority order:
 *
 * 1. **Per-request handler.** Set [sendAiRequestHandler] to a function that takes the
 *    [AiProviderClientRequest] and returns the [AiProviderClientResponse]. Useful for tests that need to
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
internal class AiProviderClientInMemoryFake : AiProviderClient {

    /** Calls received in order. Inspect after the test to assert request shape. */
    val recordedRequests: MutableList<AiProviderClientRequest> = mutableListOf()

    /** Calls to [listModels] received in order. */
    val recordedListModelsCalls: MutableList<String> = mutableListOf()

    /** Optional override that decides what to return based on the actual request. */
    var sendAiRequestHandler: ((AiProviderClientRequest) -> AiProviderClientResponse)? = null

    /** Linear queue of responses. `sendAiRequest()` pops the head. */
    val responseQueue: ArrayDeque<AiProviderClientResponse> = ArrayDeque()

    /** What [listModels] returns (for any apiKey). Override per-test. */
    var modelsResponse: List<AiProviderClientModelInfo> = emptyList()

    /** When set, [sendAiRequest] throws this instead of returning a response. */
    var sendAiRequestException: AiProviderClientException? = null

    /** When set, [listModels] throws this instead of returning [modelsResponse]. */
    var listModelsException: AiProviderClientException? = null

    override suspend fun sendAiRequest(request: AiProviderClientRequest): AiProviderClientResponse {
        recordedRequests += request
        sendAiRequestException?.let { throw it }
        sendAiRequestHandler?.let { return it(request) }
        if (responseQueue.isEmpty()) {
            throw IllegalStateException(
                "AiProviderClientInMemoryFake.sendAiRequest() called but neither sendAiRequestHandler " +
                    "nor responseQueue was configured. Test setup error."
            )
        }
        return responseQueue.removeFirst()
    }

    override suspend fun listModels(apiKey: String): List<AiProviderClientModelInfo> {
        recordedListModelsCalls += apiKey
        listModelsException?.let { throw it }
        return modelsResponse
    }

    // -- Convenience builders ----------------------------------------------------

    /** Build a text-only response (model returned prose, no tool calls, finished naturally). */
    fun textOnly(text: String, raw: JsonObject = buildJsonObject {}): AiProviderClientResponse =
        AiProviderClientResponse(
            text = text,
            toolCalls = emptyList(),
            stopReason = AiProviderClientStopReason.Stop,
            raw = raw,
        )

    /** Build a single-tool-call response (model wants the runtime to invoke one tool). */
    fun toolCall(
        toolCallId: String,
        toolName: String,
        arguments: JsonObject,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiProviderClientResponse = AiProviderClientResponse(
        text = text,
        toolCalls = listOf(ToolCall(toolCallId, toolName, arguments.toString())),
        stopReason = AiProviderClientStopReason.ToolUse,
        raw = raw,
    )

    /** Build a multi-tool-call response (model proposes several tool invocations in one turn). */
    fun multipleToolCalls(
        calls: List<ToolCall>,
        text: String? = null,
        raw: JsonObject = buildJsonObject {},
    ): AiProviderClientResponse = AiProviderClientResponse(
        text = text,
        toolCalls = calls,
        stopReason = AiProviderClientStopReason.ToolUse,
        raw = raw,
    )
}
