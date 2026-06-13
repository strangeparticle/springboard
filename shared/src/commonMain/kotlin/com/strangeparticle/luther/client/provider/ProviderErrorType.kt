package com.strangeparticle.luther.client.provider

internal enum class ProviderErrorType {
    InvalidApiKey, RateLimit, QuotaExceeded, ContextTooLarge,
    Network, ProviderUnavailable, MalformedResponse, Unknown,
}
