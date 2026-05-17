package com.strangeparticle.editio.client.provider.anthropic

import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.AiClientErrorType
import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.client.AiClientResponse
import com.strangeparticle.editio.client.provider.anthropic.request.AnthropicChatCompletionRequestDto
import com.strangeparticle.editio.client.provider.anthropic.response.AnthropicResponseParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * REST-based [AiClient] implementation for Anthropic's Messages API.
 *
 * Uses [HttpClient] (provided by the caller so the same impl works under desktop CIO
 * and any other engine) plus Anthropic DTOs / [AnthropicResponseParser] to translate
 * between provider-neutral types and the wire format. No SDK dependency.
 *
 * Per spec §3.3.
 */
internal class AiClientAnthropic(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = "https://api.anthropic.com",
) : AiClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun sendAiRequest(request: AiClientRequest): AiClientResponse {
        val apiKey = getApiKeyOrThrow()
        // AnthropicChatCompletionRequestTest contains full serialized JSON examples for this DTO boundary.
        val body = json.encodeToString(
            AnthropicChatCompletionRequestDto.serializer(),
            AnthropicChatCompletionRequestDto.from(request),
        )
        val response = postOrThrow("$baseUrl/v1/messages", apiKey, body)
        return AnthropicResponseParser.parseSuccess(response.bodyAsText())
    }

    override suspend fun listModels(apiKey: String): List<AiClientModelInfo> {
        if (apiKey.isBlank()) {
            throw AiClientException(
                AiClientErrorType.InvalidApiKey,
                "Cannot list models: API key is blank.",
            )
        }
        val response = try {
            httpClient.get("$baseUrl/v1/models") {
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", ANTHROPIC_VERSION)
                }
            }
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation must propagate so the surrounding scope
            // unwinds normally — never reclassify as a Network error.
            throw e
        } catch (e: Exception) {
            throw AiClientException(
                AiClientErrorType.Network,
                "Network error while listing Anthropic models: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            AnthropicResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        return AnthropicModelFilter.filterAndMap(AnthropicResponseParser.parseModelListResponse(response.bodyAsText()))
    }

    private fun getApiKeyOrThrow(): String {
        val key = apiKeyProvider()
        if (key.isNullOrBlank()) {
            throw AiClientException(
                AiClientErrorType.InvalidApiKey,
                "Cannot call Anthropic: API key is missing.",
            )
        }
        return key
    }

    private suspend fun postOrThrow(url: String, apiKey: String, body: String): HttpResponse {
        val response = try {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", ANTHROPIC_VERSION)
                }
                setBody(body)
            }
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation must propagate so the surrounding scope
            // unwinds normally — never reclassify as a Network error.
            throw e
        } catch (e: Exception) {
            throw AiClientException(
                AiClientErrorType.Network,
                "Network error calling Anthropic: ${e.message}",
                cause = e,
            )
        }
        if (response.status != HttpStatusCode.OK) {
            AnthropicResponseParser.parseErrorAndThrow(response.status.value, response.bodyAsText())
        }
        return response
    }

    companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
