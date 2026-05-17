package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.client.AiClientErrorType
import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.client.AiClientStopReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser]. Covers success-body parsing across the three
 * shapes (text-only, tool-call-only, mixed), finish_reason mapping, malformed
 * payloads, and the HTTP-status → [AiClientErrorType] classifier. The multiline
 * JSON bodies here are executable examples for the OpenAI response/error DTOs.
 */
internal class OpenAiResponseParserTest {

    @Test
    fun `parse text-only response`() {
        val body =
            """
            {
              "choices": [
                {
                  "message": { "role": "assistant", "content": "hello world" },
                  "finish_reason": "stop"
                }
              ]
            }
            """.trimIndent()

        val response = _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body)

        assertEquals("hello world", response.text)
        assertTrue(response.toolCalls.isEmpty())
        assertEquals(AiClientStopReason.Stop, response.stopReason)
    }

    @Test
    fun `parse tool-call-only response (no text)`() {
        val body =
            """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": null,
                    "tool_calls": [
                      {
                        "id": "call-abc",
                        "type": "function",
                        "function": { "name": "add_app", "arguments": "{\"id\":\"foo\"}" }
                      }
                    ]
                  },
                  "finish_reason": "tool_calls"
                }
              ]
            }
            """.trimIndent()

        val response = _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body)

        assertNull(response.text)
        assertEquals(1, response.toolCalls.size)
        val call = response.toolCalls.first()
        assertEquals("call-abc", call.toolCallId)
        assertEquals("add_app", call.toolName)
        assertEquals("{" + "\"id\":\"foo\"}", call.argumentsAsJsonString)
        assertEquals(AiClientStopReason.ToolUse, response.stopReason)
    }

    @Test
    fun `parse mixed text plus tool-call response`() {
        val body =
            """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "let me add that",
                    "tool_calls": [
                      {
                        "id": "call-1",
                        "type": "function",
                        "function": { "name": "add_app", "arguments": "{}" }
                      }
                    ]
                  },
                  "finish_reason": "tool_calls"
                }
              ]
            }
            """.trimIndent()

        val response = _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body)

        assertEquals("let me add that", response.text)
        assertEquals(1, response.toolCalls.size)
    }

    @Test
    fun `finish_reason length maps to MaxTokens`() {
        val body = """{ "choices": [{ "message": { "content": "..." }, "finish_reason": "length" }] }"""
        assertEquals(AiClientStopReason.MaxTokens, _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body).stopReason)
    }

    @Test
    fun `unknown finish_reason maps to Other`() {
        val body = """{ "choices": [{ "message": { "content": "..." }, "finish_reason": "content_filter" }] }"""
        assertEquals(AiClientStopReason.Other, _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body).stopReason)
    }

    @Test
    fun `parse throws MalformedResponse when choices is missing`() {
        val body = """{ "id": "x" }"""
        val ex = assertFailsWith<AiClientException> { _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiClientErrorType.MalformedResponse, ex.classified)
    }

    @Test
    fun `parse throws MalformedResponse when tool_call arguments are not valid JSON`() {
        val body =
            """
            {
              "choices": [{
                "message": {
                  "role": "assistant", "content": null,
                  "tool_calls": [
                    { "id": "x", "type": "function", "function": { "name": "f", "arguments": "not json" } }
                  ]
                },
                "finish_reason": "tool_calls"
              }]
            }
            """.trimIndent()
        val ex = assertFailsWith<AiClientException> { _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiClientErrorType.MalformedResponse, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies 401 as InvalidApiKey`() {
        val body =
            """
            {
              "error": {
                "message": "Invalid API key",
                "type": "invalid_request_error",
                "code": "invalid_api_key"
              }
            }
            """.trimIndent()

        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                401,
                body,
            )
        }
        assertEquals(AiClientErrorType.InvalidApiKey, ex.classified)
        assertEquals("Invalid API key", ex.rawProviderMessage)
    }

    @Test
    fun `parseErrorAndThrow classifies 429 as RateLimit`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(429, """{ "error": { "message": "rate limited" } }""")
        }
        assertEquals(AiClientErrorType.RateLimit, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies rate_limit_exceeded code as RateLimit`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                429,
                """{"error":{"message":"rate limited","type":"rate_limit_error","code":"rate_limit_exceeded"}}""",
            )
        }
        assertEquals(AiClientErrorType.RateLimit, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies invalid_api_key code as InvalidApiKey`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                400,
                """{"error":{"message":"bad key","type":"invalid_request_error","code":"invalid_api_key"}}""",
            )
        }
        assertEquals(AiClientErrorType.InvalidApiKey, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies 500 as ProviderUnavailable`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(500, "internal server error")
        }
        assertEquals(AiClientErrorType.ProviderUnavailable, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow with a non-JSON body still surfaces it as raw provider message`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(503, "Service Temporarily Unavailable")
        }
        // Falls back to surfacing the raw body since it can't pull a structured "message" field.
        assertNotNull(ex.rawProviderMessage)
        assertTrue(ex.rawProviderMessage!!.contains("Service"))
    }

    @Test
    fun `parseErrorAndThrow classifies context_length_exceeded code as ContextTooLarge`() {
        // OpenAI returns HTTP 400 for this case — without the body-level classification,
        // it would fall back to AiErrorClass.Unknown.
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                400,
                """{"error":{"message":"context length exceeded","type":"invalid_request_error","code":"context_length_exceeded"}}""",
            )
        }
        assertEquals(AiClientErrorType.ContextTooLarge, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies insufficient_quota code as QuotaExceeded over RateLimit`() {
        // OpenAI returns HTTP 429 for this case too — the granular classification means
        // we tell QuotaExceeded apart from a transient rate limit.
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                429,
                """{"error":{"message":"You exceeded your current quota","type":"insufficient_quota","code":"insufficient_quota"}}""",
            )
        }
        assertEquals(AiClientErrorType.QuotaExceeded, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow falls back to HTTP status when error code is unrecognized`() {
        val ex = assertFailsWith<AiClientException> {
            _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(
                429,
                """{"error":{"message":"slow down","type":"rate_limit_error","code":"some_new_unknown_code"}}""",
            )
        }
        // No code match and no type match → classify by status (429 → RateLimit).
        assertEquals(AiClientErrorType.RateLimit, ex.classified)
    }

    @Test
    fun `parseSuccess throws MalformedResponse when tool_calls contains a non-object entry`() {
        val body =
            """
            {
              "choices": [{
                "message": {
                  "role": "assistant", "content": null,
                  "tool_calls": ["not an object"]
                },
                "finish_reason": "tool_calls"
              }]
            }
            """.trimIndent()
        val ex = assertFailsWith<AiClientException> { _root_ide_package_.com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiClientErrorType.MalformedResponse, ex.classified)
    }
}
