package com.strangeparticle.springboard.app.ai.providers.openai

import com.strangeparticle.springboard.app.ai.core.AiClient
import com.strangeparticle.springboard.app.ai.core.AiErrorClass
import com.strangeparticle.springboard.app.ai.core.AiException
import com.strangeparticle.springboard.app.ai.core.AiModelInfo
import com.strangeparticle.springboard.app.ai.core.AiRequest
import com.strangeparticle.springboard.app.ai.core.AiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * REST-based [AiClient] implementation for OpenAI's chat-completions API.
 *
 * Uses [HttpClient] (provided by the caller so the same impl works under desktop CIO
 * and any other engine) plus the pure [OpenAiRequestBuilder] / [OpenAiResponseParser]
 * to translate between provider-neutral types and the wire format. No SDK dependency.
 *
 * Per spec §3.3.
 */
internal class AiClientOpenAi(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = "https://api.openai.com",
) : AiClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun sendAiRequest(request: AiRequest): AiResponse {
        val apiKey = getApiKeyOrThrow()
        val body = OpenAiRequestBuilder.buildBody(request)
        val response = postOrThrow("$baseUrl/v1/chat/completions", apiKey, body)
        val parsedBody = parseJsonOrThrow(response)
        return OpenAiResponseParser.parseSuccess(parsedBody)
    }

    override suspend fun listModels(apiKey: String): List<AiModelInfo> {
        if (apiKey.isBlank()) {
            throw AiException(
                AiErrorClass.InvalidApiKey,
                "Cannot list models: API key is blank.",
            )
        }
        val response = try {
            httpClient.get("$baseUrl/v1/models") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
            }
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation must propagate so the surrounding scope
            // unwinds normally — never reclassify as a Network error.
            throw e
        } catch (e: Exception) {
            throw AiException(
                AiErrorClass.Network,
                "Network error while listing OpenAI models: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            OpenAiResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        val parsed = parseJsonOrThrow(response)
        return OpenAiModelFilter.filterAndMap(parsed)
    }

    private fun getApiKeyOrThrow(): String {
        val key = apiKeyProvider()
        if (key.isNullOrBlank()) {
            throw AiException(
                AiErrorClass.InvalidApiKey,
                "Cannot call OpenAI: API key is missing.",
            )
        }
        return key
    }

    private suspend fun postOrThrow(url: String, apiKey: String, body: JsonObject): HttpResponse {
        val response = try {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(body.toString())
            }
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation must propagate so the surrounding scope
            // unwinds normally — never reclassify as a Network error.
            throw e
        } catch (e: Exception) {
            throw AiException(
                AiErrorClass.Network,
                "Network error calling OpenAI: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            OpenAiResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        return response
    }

    private suspend fun parseJsonOrThrow(response: HttpResponse): JsonObject {
        val raw = response.bodyAsText()
        return try {
            json.parseToJsonElement(raw) as JsonObject
        } catch (e: Exception) {
            throw AiException(
                AiErrorClass.MalformedResponse,
                "OpenAI response was not valid JSON: ${e.message}",
                rawProviderMessage = raw,
                cause = e,
            )
        }
    }
}
