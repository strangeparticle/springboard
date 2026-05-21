package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.client.AiProviderClientErrorType
import com.strangeparticle.editio.client.AiProviderClientException
import com.strangeparticle.editio.client.AiProviderClientRequest
import com.strangeparticle.editio.client.AiProviderClientStopReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for [com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi]. Wires the client to a Ktor [MockEngine] so we can
 * assert on the request shape (URL, headers, body) and inject the responses we
 * want.
 */
internal class AiProviderClientOpenAiTest {

    private fun emptyRequest() = AiProviderClientRequest(
        modelId = "gpt-5",
        systemPrompt = "you are an assistant",
        history = emptyList(),
        tools = emptyList(),
    )

    private fun mockClient(
        respondWith: (io.ktor.client.request.HttpRequestData) -> io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.client.request.HttpResponseData,
    ): HttpClient {
        val engine = MockEngine { request ->
            respondWith(request)()
        }
        return HttpClient(engine)
    }

    @Test
    fun `sendAiRequest posts to chat completions endpoint with bearer auth`() = runTest {
        var capturedUrl: String? = null
        var capturedAuth: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"choices":[{"message":{"content":"hi"},"finish_reason":"stop"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        sut.sendAiRequest(emptyRequest())

        assertEquals("https://api.openai.com/v1/chat/completions", capturedUrl)
        assertEquals("Bearer sk-test", capturedAuth)
    }

    @Test
    fun `sendAiRequest returns parsed response on 200`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"choices":[{"message":{"content":"hello"},"finish_reason":"stop"}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val response = sut.sendAiRequest(emptyRequest())

        assertEquals("hello", response.text)
        assertTrue(response.toolCalls.isEmpty())
        assertEquals(AiProviderClientStopReason.Stop, response.stopReason)
    }

    @Test
    fun `sendAiRequest throws InvalidApiKey on 401`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"error":{"message":"Invalid API key"}}""",
                HttpStatusCode.Unauthorized,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val ex = assertFailsWith<AiProviderClientException> { sut.sendAiRequest(emptyRequest()) }
        assertEquals(AiProviderClientErrorType.InvalidApiKey, ex.classified)
        assertEquals("Invalid API key", ex.rawProviderMessage)
    }

    @Test
    fun `sendAiRequest throws RateLimit on 429`() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"error":{"message":"slow down"}}""", HttpStatusCode.TooManyRequests)
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val ex = assertFailsWith<AiProviderClientException> { sut.sendAiRequest(emptyRequest()) }
        assertEquals(AiProviderClientErrorType.RateLimit, ex.classified)
    }

    @Test
    fun `sendAiRequest throws ProviderUnavailable on 5xx`() = runTest {
        val client = HttpClient(MockEngine {
            respond("internal", HttpStatusCode.InternalServerError)
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val ex = assertFailsWith<AiProviderClientException> { sut.sendAiRequest(emptyRequest()) }
        assertEquals(AiProviderClientErrorType.ProviderUnavailable, ex.classified)
    }

    @Test
    fun `sendAiRequest throws Network on transport exception`() = runTest {
        val client = HttpClient(MockEngine {
            throw RuntimeException("connection refused")
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val ex = assertFailsWith<AiProviderClientException> { sut.sendAiRequest(emptyRequest()) }
        assertEquals(AiProviderClientErrorType.Network, ex.classified)
    }

    @Test
    fun `sendAiRequest throws InvalidApiKey when apiKey is null`() = runTest {
        val client = HttpClient(MockEngine {
            error("sendAiRequest should never call the engine when api key is missing")
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { null })

        val ex = assertFailsWith<AiProviderClientException> { sut.sendAiRequest(emptyRequest()) }
        assertEquals(AiProviderClientErrorType.InvalidApiKey, ex.classified)
    }

    @Test
    fun `listModels GETs models endpoint and returns filtered, sorted models`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedUrl: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            capturedUrl = request.url.toString()
            respond(
                """
                {"object":"list","data":[
                  {"id":"gpt-5","object":"model"},
                  {"id":"text-embedding-3-large","object":"model"},
                  {"id":"gpt-4o","object":"model"}
                ]}
                """.trimIndent(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-not-used-for-listModels" })

        val models = sut.listModels("sk-list-test")

        assertEquals(HttpMethod.Get, capturedMethod)
        assertEquals("https://api.openai.com/v1/models", capturedUrl)
        assertEquals(listOf("gpt-5", "gpt-4o"), models.map { it.id })
    }

    @Test
    fun `listModels throws InvalidApiKey on blank apiKey`() = runTest {
        val client = HttpClient(MockEngine {
            error("should not reach engine when api key is blank")
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { null })

        val ex = assertFailsWith<AiProviderClientException> { sut.listModels("") }
        assertEquals(AiProviderClientErrorType.InvalidApiKey, ex.classified)
    }

    @Test
    fun `listModels throws InvalidApiKey on 401`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"error":{"message":"bad key"}}""",
                HttpStatusCode.Unauthorized,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        val ex = assertFailsWith<AiProviderClientException> { sut.listModels("sk-test") }
        assertEquals(AiProviderClientErrorType.InvalidApiKey, ex.classified)
    }

    @Test
    fun `sendAiRequest propagates CancellationException without reclassifying as Network`() = runTest {
        val client = HttpClient(MockEngine {
            // Simulate the in-flight request being cancelled — the engine throws
            // a CancellationException, which should bubble up uncaught.
            throw kotlinx.coroutines.CancellationException("turn cancelled by user")
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        // The expectation is the CancellationException — NOT an AiException.
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            sut.sendAiRequest(emptyRequest())
        }
    }

    @Test
    fun `listModels propagates CancellationException without reclassifying as Network`() = runTest {
        val client = HttpClient(MockEngine {
            throw kotlinx.coroutines.CancellationException("settings dialog cancelled fetch")
        })
        val sut =
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.AiProviderClientOpenAi(
                client,
                apiKeyProvider = { "sk-test" })

        assertFailsWith<kotlinx.coroutines.CancellationException> {
            sut.listModels("sk-test")
        }
    }
}
