package com.strangeparticle.luther.client.provider.openai

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import com.strangeparticle.luther.client.AiProviderClientErrorType
import com.strangeparticle.luther.client.AiProviderClientException
import com.strangeparticle.luther.client.AiProviderClientRequest
import com.strangeparticle.luther.client.AiProviderClientResponse
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
 * REST-based [AiProviderClient] implementation for OpenAI's chat-completions API.
 *
  * Uses [HttpClient] (provided by the caller so the same impl works under desktop CIO
  * and any other engine) plus OpenAI DTOs / [com.strangeparticle.luther.client.provider.openai.response.OpenAiResponseParser]
  * to translate between provider-neutral types and the wire format. No SDK dependency.
 *
 * Per spec §3.3.
 */
internal class AiProviderClientOpenAi(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = "https://api.openai.com",
) : AiProviderClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun sendAiRequest(request: AiProviderClientRequest): AiProviderClientResponse {
        val apiKey = getApiKeyOrThrow()
        // OpenAiChatCompletionRequestTest contains full serialized JSON examples for this DTO boundary.
        val body = json.encodeToString(
            _root_ide_package_.com.strangeparticle.luther.client.provider.openai.request.OpenAiChatCompletionRequestDto.Companion.serializer(),
            _root_ide_package_.com.strangeparticle.luther.client.provider.openai.request.OpenAiChatCompletionRequestDto.Companion.from(request),
        )
        val response = postOrThrow("$baseUrl/v1/chat/completions", apiKey, body)
        return _root_ide_package_.com.strangeparticle.luther.client.provider.openai.response.OpenAiResponseParser.parseSuccess(response.bodyAsText())
    }

    override suspend fun listModels(apiKey: String): List<AiProviderClientModelInfo> {
        if (apiKey.isBlank()) {
            throw AiProviderClientException(
                AiProviderClientErrorType.InvalidApiKey,
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
            throw AiProviderClientException(
                AiProviderClientErrorType.Network,
                "Network error while listing OpenAI models: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            _root_ide_package_.com.strangeparticle.luther.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        val parsed = parseJsonOrThrow(response)
        return _root_ide_package_.com.strangeparticle.luther.client.provider.openai.OpenAiModelFilter.filterAndMap(parsed)
    }

    private fun getApiKeyOrThrow(): String {
        val key = apiKeyProvider()
        if (key.isNullOrBlank()) {
            throw AiProviderClientException(
                AiProviderClientErrorType.InvalidApiKey,
                "Cannot call OpenAI: API key is missing.",
            )
        }
        return key
    }

    private suspend fun postOrThrow(url: String, apiKey: String, body: String): HttpResponse {
        val response = try {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(body)
            }
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation must propagate so the surrounding scope
            // unwinds normally — never reclassify as a Network error.
            throw e
        } catch (e: Exception) {
            throw AiProviderClientException(
                AiProviderClientErrorType.Network,
                "Network error calling OpenAI: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            _root_ide_package_.com.strangeparticle.luther.client.provider.openai.response.OpenAiResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        return response
    }

    private suspend fun parseJsonOrThrow(response: HttpResponse): JsonObject {
        val raw = response.bodyAsText()
        return try {
            json.parseToJsonElement(raw) as JsonObject
        } catch (e: Exception) {
            throw AiProviderClientException(
                AiProviderClientErrorType.MalformedResponse,
                "OpenAI response was not valid JSON: ${e.message}",
                rawProviderMessage = raw,
                cause = e,
            )
        }
    }
}
