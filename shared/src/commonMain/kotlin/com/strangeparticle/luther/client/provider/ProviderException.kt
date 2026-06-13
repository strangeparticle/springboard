package com.strangeparticle.luther.client.provider

class ProviderException(
    val classified: ProviderErrorType,
    message: String,
    val rawProviderMessage: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
