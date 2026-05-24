package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.HttpClientTimeoutConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class HttpClientFactoryTest {

    @Test
    fun `timeout config converts seconds to milliseconds`() {
        val config = HttpClientTimeoutConfig.fromSeconds(requestAndSocketTimeoutSeconds = 180)

        assertEquals(15_000, config.connectTimeoutMillis)
        assertEquals(180_000, config.requestTimeoutMillis)
        assertEquals(180_000, config.socketTimeoutMillis)
    }

    @Test
    fun `timeout config can be built from explicit duration`() {
        val config = HttpClientTimeoutConfig.fromDuration(requestAndSocketTimeout = 30.seconds)

        assertEquals(30_000, config.requestTimeoutMillis)
        assertEquals(30_000, config.socketTimeoutMillis)
    }
}
