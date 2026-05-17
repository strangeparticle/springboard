package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.client.AiClientErrorType
import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.client.AiClientStopReason
import com.strangeparticle.editio.client.provider.anthropic.AiClientAnthropic
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [AiClientAnthropic]. Wires the client to a Ktor [MockEngine] so we can assert
 * on request shape (URL, headers, body) and inject the provider responses we want.
 */
internal class AiClientAnthropicTest {

    private fun emptyRequest() = AiClientRequest(
        modelId = "claude-sonnet-4-6",
        systemPrompt = "you are an assistant",
        history = emptyList(),
        tools = emptyList(),
    )

    private val successBody = """
        {
          "id": "msg_01",
          "type": "message",
          "role": "assistant",
          "model": "claude-sonnet-4-6",
          "content": [{"type": "text", "text": "hello"}],
          "stop_reason": "end_turn",
          "usage": {"input_tokens": 10, "output_tokens": 5}
        }
    """.trimIndent()

    private val modelListBody = """
        {"data":[{"type":"model","id":"claude-sonnet-4-6","display_name":"Claude Sonnet 4.6"}]}
    """.trimIndent()

    // ── sendAiRequest ─────────────────────────────────────────────────────────

    @Test
    fun `sendAiRequest posts to messages endpoint`() = runTest {
        var capturedUrl: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(successBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())

        assertEquals("https://api.anthropic.com/v1/messages", capturedUrl)
    }

    @Test
    fun `sendAiRequest includes x-api-key header`() = runTest {
        var capturedKey: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedKey = request.headers["x-api-key"]
            respond(successBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())

        assertEquals("sk-ant-test", capturedKey)
    }

    @Test
    fun `sendAiRequest includes anthropic-version header`() = runTest {
        var capturedVersion: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedVersion = request.headers["anthropic-version"]
            respond(successBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())

        assertEquals(AiClientAnthropic.ANTHROPIC_VERSION, capturedVersion)
    }

    @Test
    fun `sendAiRequest does not include Authorization Bearer header`() = runTest {
        var capturedAuth: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(successBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())

        assertTrue(capturedAuth == null, "Anthropic uses x-api-key, not Authorization: Bearer")
    }

    @Test
    fun `sendAiRequest returns parsed response on success`() = runTest {
        val client = HttpClient(MockEngine { respond(successBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) })
        val result = AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())

        assertEquals("hello", result.text)
        assertEquals(AiClientStopReason.Stop, result.stopReason)
    }

    @Test
    fun `sendAiRequest throws InvalidApiKey when key is missing`() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { null }).sendAiRequest(emptyRequest())
        }
        assertEquals(AiClientErrorType.InvalidApiKey, error.classified)
    }

    @Test
    fun `sendAiRequest classifies 401 as InvalidApiKey`() = runTest {
        val body = """{"type":"error","error":{"type":"authentication_error","message":"Invalid key"}}"""
        val client = HttpClient(MockEngine { respond(body, HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json")) })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { "bad-key" }).sendAiRequest(emptyRequest())
        }
        assertEquals(AiClientErrorType.InvalidApiKey, error.classified)
    }

    @Test
    fun `sendAiRequest classifies 429 as RateLimit`() = runTest {
        val body = """{"type":"error","error":{"type":"rate_limit_error","message":"Too many requests"}}"""
        val client = HttpClient(MockEngine { respond(body, HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.ContentType, "application/json")) })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())
        }
        assertEquals(AiClientErrorType.RateLimit, error.classified)
    }

    @Test
    fun `sendAiRequest classifies 529 as ProviderUnavailable`() = runTest {
        val body = """{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}"""
        val client = HttpClient(MockEngine { respond(body, HttpStatusCode(529, "Overloaded"), headersOf(HttpHeaders.ContentType, "application/json")) })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())
        }
        assertEquals(AiClientErrorType.ProviderUnavailable, error.classified)
    }

    @Test
    fun `sendAiRequest classifies network exception as Network error`() = runTest {
        val client = HttpClient(MockEngine { throw Exception("connection refused") })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())
        }
        assertEquals(AiClientErrorType.Network, error.classified)
    }

    @Test
    fun `sendAiRequest propagates CancellationException`() = runTest {
        val client = HttpClient(MockEngine { throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> {
            AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).sendAiRequest(emptyRequest())
        }
    }

    // ── listModels ────────────────────────────────────────────────────────────

    @Test
    fun `listModels returns filtered and sorted models`() = runTest {
        val client = HttpClient(MockEngine { respond(modelListBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) })
        val models = AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).listModels("sk-ant-test")

        assertEquals(1, models.size)
        assertEquals("claude-sonnet-4-6", models[0].id)
    }

    @Test
    fun `listModels includes x-api-key and anthropic-version headers`() = runTest {
        var capturedKey: String? = null
        var capturedVersion: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedKey = request.headers["x-api-key"]
            capturedVersion = request.headers["anthropic-version"]
            respond(modelListBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        AiClientAnthropic(client, apiKeyProvider = { "sk-ant-test" }).listModels("sk-ant-test")

        assertEquals("sk-ant-test", capturedKey)
        assertEquals(AiClientAnthropic.ANTHROPIC_VERSION, capturedVersion)
    }

    @Test
    fun `listModels throws InvalidApiKey when key is blank`() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val error = assertFailsWith<AiClientException> {
            AiClientAnthropic(client, apiKeyProvider = { "" }).listModels("")
        }
        assertEquals(AiClientErrorType.InvalidApiKey, error.classified)
    }
}
