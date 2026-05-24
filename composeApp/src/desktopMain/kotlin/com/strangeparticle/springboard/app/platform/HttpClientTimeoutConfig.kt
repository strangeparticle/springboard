package com.strangeparticle.springboard.app.platform

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class HttpClientTimeoutConfig(
    val connectTimeoutMillis: Long,
    val requestTimeoutMillis: Long,
    val socketTimeoutMillis: Long,
) {
    companion object {
        fun fromSeconds(requestAndSocketTimeoutSeconds: Int): HttpClientTimeoutConfig =
            fromDuration(requestAndSocketTimeoutSeconds.seconds)

        fun fromDuration(requestAndSocketTimeout: Duration): HttpClientTimeoutConfig =
            HttpClientTimeoutConfig(
                connectTimeoutMillis = 15_000,
                requestTimeoutMillis = requestAndSocketTimeout.inWholeMilliseconds,
                socketTimeoutMillis = requestAndSocketTimeout.inWholeMilliseconds,
            )
    }
}
