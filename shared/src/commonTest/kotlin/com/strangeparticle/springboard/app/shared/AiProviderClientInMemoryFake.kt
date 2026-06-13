package com.strangeparticle.springboard.app.shared

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.provider.ChatRequest
import com.strangeparticle.luther.client.provider.ChatResponse
import com.strangeparticle.luther.client.provider.Model
import com.strangeparticle.luther.client.provider.ProviderException
import com.strangeparticle.luther.client.provider.StopReason
import com.strangeparticle.luther.client.provider.ToolCall
import kotlinx.serialization.json.JsonObject

/**
 * Test double for [AiProviderClient]. Tests script the responses they want; the fake records
 * everything that flows through it so assertions can verify request shape without
 * touching Ktor or any HTTP transport.
 *
 * Two ways to script behavior, in priority order:
 *
 * 1. **Per-request handler.** Set [sendAiRequestHandler] to a function that takes the
 *    [ChatRequest] and returns the [ChatResponse]. Useful for tests that need to
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
    val recordedRequests: MutableList<ChatRequest> = mutableListOf()

    /** Number of times [listModels] has been called. */
    var listModelsCallCount: Int = 0

    /** Optional override that decides what to return based on the actual request. */
    var sendAiRequestHandler: ((ChatRequest) -> ChatResponse)? = null

    /** Linear queue of responses. `sendAiRequest()` pops the head. */
    val responseQueue: ArrayDeque<ChatResponse> = ArrayDeque()

    /** What [listModels] returns. Override per-test. */
    var modelsResponse: List<Model> = emptyList()

    /** When set, [sendAiRequest] throws this instead of returning a response. */
    var sendAiRequestException: ProviderException? = null

    /** When set, [listModels] throws this instead of returning [modelsResponse]. */
    var listModelsException: ProviderException? = null

    override suspend fun sendAiRequest(request: ChatRequest): ChatResponse {
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

    override suspend fun listModels(): List<Model> {
        listModelsCallCount++
        listModelsException?.let { throw it }
        return modelsResponse
    }

    // -- Convenience builders ----------------------------------------------------

    /** Build a text-only response (model returned prose, no tool calls, finished naturally). */
    fun textOnly(text: String): ChatResponse =
        ChatResponse(
            text = text,
            toolCalls = emptyList(),
            stopReason = StopReason.Stop,
        )

    /** Build a single-tool-call response (model wants the runtime to invoke one tool). */
    fun toolCall(
        toolCallId: String,
        toolName: String,
        arguments: JsonObject,
        text: String? = null,
    ): ChatResponse = ChatResponse(
        text = text,
        toolCalls = listOf(ToolCall(toolCallId, toolName, arguments.toString())),
        stopReason = StopReason.ToolUse,
    )

    /** Build a multi-tool-call response (model proposes several tool invocations in one turn). */
    fun multipleToolCalls(
        calls: List<ToolCall>,
        text: String? = null,
    ): ChatResponse = ChatResponse(
        text = text,
        toolCalls = calls,
        stopReason = StopReason.ToolUse,
    )
}
