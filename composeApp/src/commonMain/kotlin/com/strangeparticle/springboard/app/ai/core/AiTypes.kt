package com.strangeparticle.springboard.app.ai.core

import kotlinx.serialization.json.JsonObject

/**
 * Provider-neutral types for the AI client. These deliberately do NOT mention any
 * provider-specific wire shapes (`tool_use`, `tool_calls`, `tool_choice`, `tool_result`,
 * etc.) — those live inside provider packages (`ai.providers.openai`,
 * `ai.providers.anthropic`) and are translated to/from these neutral types at the
 * `AiClient` boundary.
 *
 * Per spec §3.3.
 */

/** A request to a provider for a single completion. */
internal data class AiRequest(
    val modelId: String,
    val systemPrompt: String,
    val history: List<AiMessage>,
    val tools: List<AiToolDefinition>,
)

/**
 * The single full response from one round-trip with a provider. No streaming events
 * in MVP — `sendAiRequest()` resolves once, with everything the model produced.
 */
internal data class AiResponse(
    val text: String?,
    val toolCalls: List<AiToolCall>,
    val stopReason: AiStopReason,
    val raw: JsonObject,
)

/** A single tool invocation requested by the model. */
internal data class AiToolCall(
    val toolCallId: String,
    val toolName: String,
    val arguments: JsonObject,
)

/**
 * Provider-neutral conversation message types. Each provider impl translates these
 * to/from its native message envelope.
 */
internal sealed class AiMessage {
    data class UserMessage(val text: String) : AiMessage()

    data class AssistantMessage(
        val text: String?,
        val toolCalls: List<AiToolCall> = emptyList(),
    ) : AiMessage()

    data class ToolMessage(
        val toolCallId: String,
        val content: String,
    ) : AiMessage()

    /**
     * Synthetic state-injection message. Used by [AiSessionManager] to feed the
     * latest [AppStateSnapshot] back to the model when state has changed since the
     * last snapshot was sent. Each provider chooses how to surface this in its native
     * envelope (e.g. as a `user` role message wrapping a `<current_state>` block).
     */
    data class SystemStateMessage(val snapshotJson: String) : AiMessage()
}

/**
 * Description of a tool the provider should expose to the model on this request.
 * `schema` is a JSON Schema [JsonObject] describing the tool's input arguments.
 * Provider impls adapt this into their native tool-definition envelope.
 */
internal data class AiToolDefinition(
    val name: String,
    val description: String,
    val schema: JsonObject,
)

/** A model surfaced by [AiClient.listModels], suitable for showing in a settings dropdown. */
internal data class AiModelInfo(
    val id: String,
    val displayName: String?,
    val supportsToolCalling: Boolean,
)

/** Why a completion stopped. */
internal enum class AiStopReason {
    /** The model finished naturally without further tool calls. */
    Stop,

    /** The model emitted one or more tool calls and is waiting for the results. */
    ToolUse,

    /** The model hit its max-tokens budget before finishing. */
    MaxTokens,

    /** Anything else the provider reports (rare, kept for completeness). */
    Other,
}

/** Classification of a provider error so callers can react without parsing strings. */
internal enum class AiErrorClass {
    InvalidApiKey,
    RateLimit,
    QuotaExceeded,
    ContextTooLarge,
    Network,
    ProviderUnavailable,
    MalformedResponse,
    Unknown,
}

/**
 * Thrown by [AiClient] when a request fails. [classified] gives the broad category;
 * [rawProviderMessage] (when set) is the verbatim error message from the provider —
 * useful in the chat UI as a second-line "raw error" display.
 */
internal class AiException(
    val classified: AiErrorClass,
    message: String,
    val rawProviderMessage: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
