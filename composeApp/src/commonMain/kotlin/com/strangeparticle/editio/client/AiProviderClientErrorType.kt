package com.strangeparticle.editio.client

/** Classification of an AI provider client error so callers can react without parsing strings. */
internal enum class AiProviderClientErrorType {
    InvalidApiKey,
    RateLimit,
    QuotaExceeded,
    ContextTooLarge,
    Network,
    ProviderUnavailable,
    MalformedResponse,
    Unknown,
}
