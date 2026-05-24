package com.strangeparticle.springboard.app.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

object HttpClientFactory {
    fun createCioClient(timeoutConfig: HttpClientTimeoutConfig): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = timeoutConfig.connectTimeoutMillis
                requestTimeoutMillis = timeoutConfig.requestTimeoutMillis
                socketTimeoutMillis = timeoutConfig.socketTimeoutMillis
            }
        }
}
