package com.strangeparticle.luther.client.provider

enum class ProviderErrorType {
    InvalidApiKey, RateLimit, QuotaExceeded, ContextTooLarge,
    Network, ProviderUnavailable, MalformedResponse, Unknown,
}
