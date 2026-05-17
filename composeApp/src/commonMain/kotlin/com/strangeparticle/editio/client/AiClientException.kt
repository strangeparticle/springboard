package com.strangeparticle.editio.client

/**
 * Thrown by AI provider clients when a request fails. [classified] gives the broad category;
 * [rawProviderMessage] is the provider-supplied message when one is available.
 */
internal class AiClientException(
    val classified: AiClientErrorType,
    message: String,
    val rawProviderMessage: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
