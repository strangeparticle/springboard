package com.strangeparticle.springboard.app.ai.core

/**
 * Single-shot AI client. Implementations wrap a specific provider's REST API and
 * translate between [AiRequest] / [AiResponse] (provider-neutral) and the provider's
 * native envelope. Provider-specific wire shapes never escape the impl.
 *
 * No streaming in MVP — [sendAiRequest] resolves with a single full response. Streaming
 * is a documented future-phase addition.
 *
 * Per spec §3.3.
 */
internal interface AiClient {

    /**
     * Send [request] and suspend until the provider returns a single full response.
     * Throws [AiException] on transport / authentication / parse / provider errors.
     */
    suspend fun sendAiRequest(request: AiRequest): AiResponse

    /**
     * List the chat-completion-capable models the provider exposes for [apiKey].
     * Used by the AI settings screen to populate the model dropdown after the user
     * enters a key. Throws [AiException] if the key is invalid or the network is
     * unreachable.
     */
    suspend fun listModels(apiKey: String): List<AiModelInfo>
}
