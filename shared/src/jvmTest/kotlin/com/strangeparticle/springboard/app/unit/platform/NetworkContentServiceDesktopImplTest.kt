package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.NetworkContentServiceDesktopImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkContentServiceDesktopImplTest {

    @Test
    fun `fetchText uses injected http client`() = runTest {
        val service = NetworkContentServiceDesktopImpl(
            HttpClient(MockEngine { respond("springboard", HttpStatusCode.OK) }),
        )

        assertEquals("springboard", service.fetchText("https://example.com/springboard.json"))
    }
}
