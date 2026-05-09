package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.ai.core.AiErrorClass
import com.strangeparticle.springboard.app.ai.core.AiException
import com.strangeparticle.springboard.app.ai.core.AiStopReason
import com.strangeparticle.springboard.app.ai.providers.openai.OpenAiResponseParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [OpenAiResponseParser]. Covers success-body parsing across the three
 * shapes (text-only, tool-call-only, mixed), finish_reason mapping, malformed
 * payloads, and the HTTP-status → [AiErrorClass] classifier.
 */
internal class OpenAiResponseParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parseBody(rawJson: String): JsonObject =
        json.parseToJsonElement(rawJson) as JsonObject

    @Test
    fun `parse text-only response`() {
        val body = parseBody(
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
        )

        val response = OpenAiResponseParser.parseSuccess(body)

        assertEquals("hello world", response.text)
        assertTrue(response.toolCalls.isEmpty())
        assertEquals(AiStopReason.Stop, response.stopReason)
    }

    @Test
    fun `parse tool-call-only response (no text)`() {
        val body = parseBody(
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
        )

        val response = OpenAiResponseParser.parseSuccess(body)

        assertNull(response.text)
        assertEquals(1, response.toolCalls.size)
        val call = response.toolCalls.first()
        assertEquals("call-abc", call.toolCallId)
        assertEquals("add_app", call.toolName)
        assertEquals("foo", (call.arguments["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(AiStopReason.ToolUse, response.stopReason)
    }

    @Test
    fun `parse mixed text plus tool-call response`() {
        val body = parseBody(
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
        )

        val response = OpenAiResponseParser.parseSuccess(body)

        assertEquals("let me add that", response.text)
        assertEquals(1, response.toolCalls.size)
    }

    @Test
    fun `finish_reason length maps to MaxTokens`() {
        val body = parseBody(
            """{ "choices": [{ "message": { "content": "..." }, "finish_reason": "length" }] }"""
        )
        assertEquals(AiStopReason.MaxTokens, OpenAiResponseParser.parseSuccess(body).stopReason)
    }

    @Test
    fun `unknown finish_reason maps to Other`() {
        val body = parseBody(
            """{ "choices": [{ "message": { "content": "..." }, "finish_reason": "content_filter" }] }"""
        )
        assertEquals(AiStopReason.Other, OpenAiResponseParser.parseSuccess(body).stopReason)
    }

    @Test
    fun `parse throws MalformedResponse when choices is missing`() {
        val body = parseBody("""{ "id": "x" }""")
        val ex = assertFailsWith<AiException> { OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiErrorClass.MalformedResponse, ex.classified)
    }

    @Test
    fun `parse throws MalformedResponse when tool_call arguments are not valid JSON`() {
        val body = parseBody(
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
        )
        val ex = assertFailsWith<AiException> { OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiErrorClass.MalformedResponse, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies 401 as InvalidApiKey`() {
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(
                401,
                """{ "error": { "message": "Invalid API key", "type": "invalid_request_error" } }""",
            )
        }
        assertEquals(AiErrorClass.InvalidApiKey, ex.classified)
        assertEquals("Invalid API key", ex.rawProviderMessage)
    }

    @Test
    fun `parseErrorAndThrow classifies 429 as RateLimit`() {
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(429, """{ "error": { "message": "rate limited" } }""")
        }
        assertEquals(AiErrorClass.RateLimit, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies 500 as ProviderUnavailable`() {
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(500, "internal server error")
        }
        assertEquals(AiErrorClass.ProviderUnavailable, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow with a non-JSON body still surfaces it as raw provider message`() {
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(503, "Service Temporarily Unavailable")
        }
        // Falls back to surfacing the raw body since it can't pull a structured "message" field.
        assertNotNull(ex.rawProviderMessage)
        assertTrue(ex.rawProviderMessage!!.contains("Service"))
    }

    @Test
    fun `parseErrorAndThrow classifies context_length_exceeded code as ContextTooLarge`() {
        // OpenAI returns HTTP 400 for this case — without the body-level classification,
        // it would fall back to AiErrorClass.Unknown.
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(
                400,
                """{"error":{"message":"context length exceeded","type":"invalid_request_error","code":"context_length_exceeded"}}""",
            )
        }
        assertEquals(AiErrorClass.ContextTooLarge, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow classifies insufficient_quota code as QuotaExceeded over RateLimit`() {
        // OpenAI returns HTTP 429 for this case too — the granular classification means
        // we tell QuotaExceeded apart from a transient rate limit.
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(
                429,
                """{"error":{"message":"You exceeded your current quota","type":"insufficient_quota","code":"insufficient_quota"}}""",
            )
        }
        assertEquals(AiErrorClass.QuotaExceeded, ex.classified)
    }

    @Test
    fun `parseErrorAndThrow falls back to HTTP status when error code is unrecognized`() {
        val ex = assertFailsWith<AiException> {
            OpenAiResponseParser.parseErrorAndThrow(
                429,
                """{"error":{"message":"slow down","type":"rate_limit_error","code":"some_new_unknown_code"}}""",
            )
        }
        // No code match and no type match → classify by status (429 → RateLimit).
        assertEquals(AiErrorClass.RateLimit, ex.classified)
    }

    @Test
    fun `parseSuccess throws MalformedResponse when tool_calls contains a non-object entry`() {
        val body = parseBody(
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
        )
        val ex = assertFailsWith<AiException> { OpenAiResponseParser.parseSuccess(body) }
        assertEquals(AiErrorClass.MalformedResponse, ex.classified)
    }
}
