package com.strangeparticle.luther.client

/** Why a model turn stopped. */
internal enum class AiProviderClientStopReason {
    /** The model finished naturally without further tool calls. */
    Stop,

    /** The model emitted one or more tool calls and is waiting for the results. */
    ToolUse,

    /** The model hit its max-tokens budget before finishing. */
    MaxTokens,

    /** Anything else the provider reports. */
    Other,
}
