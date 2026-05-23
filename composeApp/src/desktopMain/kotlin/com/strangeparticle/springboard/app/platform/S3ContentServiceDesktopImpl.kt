package com.strangeparticle.springboard.app.platform

import com.strangeparticle.springboard.app.aws.AwsCredentialProvider
import com.strangeparticle.springboard.app.aws.AwsSigV4Signer
import com.strangeparticle.springboard.app.aws.S3UrlParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class S3ContentServiceDesktopImpl(
    private val httpClient: HttpClient,
    private val credentialProvider: AwsCredentialProvider,
    private val clock: () -> Instant = { Clock.System.now() },
) : S3ContentService {

    override suspend fun getObject(url: String, profile: String): S3GetResult {
        if (S3UrlParser.region(url) == null) {
            return S3GetResult.Failed("Not a virtual-hosted S3 URL: $url")
        }
        val credentials = credentialProvider.resolve(profile)
            ?: return S3GetResult.CredentialsUnavailable(
                "AWS credentials are not available for profile '$profile'."
            )
        val signedHeaders = AwsSigV4Signer.sign(
            method = "GET",
            url = url,
            additionalSignedHeaders = emptyMap(),
            bodyBytes = ByteArray(0),
            credentials = credentials,
            service = "s3",
            signingInstant = clock(),
        )

        val response = try {
            httpClient.get(url) {
                headers {
                    signedHeaders.forEach { (name, value) -> append(name, value) }
                }
            }
        } catch (e: Exception) {
            return S3GetResult.Failed(e.message ?: "Network error")
        }
        val status = response.status.value
        return when {
            status in 200..299 -> S3GetResult.Success(response.bodyAsText(), extractEtag(response))
            status == 403 -> S3GetResult.Denied(extractS3ErrorMessage(response, "Access denied"))
            status == 404 -> S3GetResult.Failed("Object not found")
            else -> S3GetResult.Failed("HTTP $status: ${extractS3ErrorMessage(response, response.status.description)}")
        }
    }

    override suspend fun putObject(url: String, profile: String, content: String, ifMatch: String?): S3PutResult {
        if (S3UrlParser.region(url) == null) {
            return S3PutResult.Failed("Not a virtual-hosted S3 URL: $url")
        }
        val credentials = credentialProvider.resolve(profile)
            ?: return S3PutResult.CredentialsUnavailable(
                "AWS credentials are not available for profile '$profile'."
            )

        val bodyBytes = content.toByteArray(Charsets.UTF_8)
        val additionalHeaders = buildMap {
            if (ifMatch != null) put("If-Match", ifMatch)
        }
        val signedHeaders = AwsSigV4Signer.sign(
            method = "PUT",
            url = url,
            additionalSignedHeaders = additionalHeaders,
            bodyBytes = bodyBytes,
            credentials = credentials,
            service = "s3",
            signingInstant = clock(),
        )

        val response = try {
            httpClient.put(url) {
                headers {
                    signedHeaders.forEach { (name, value) -> append(name, value) }
                    additionalHeaders.forEach { (name, value) -> append(name, value) }
                }
                setBody(bodyBytes)
            }
        } catch (e: Exception) {
            return S3PutResult.Failed(e.message ?: "Network error")
        }
        val status = response.status.value
        return when {
            status in 200..299 -> S3PutResult.Success(extractEtag(response))
            status == 412 -> S3PutResult.Conflict("File was modified externally since last load")
            status == 403 -> S3PutResult.Denied(extractS3ErrorMessage(response, "Access denied"))
            else -> S3PutResult.Failed("HTTP $status: ${extractS3ErrorMessage(response, response.status.description)}")
        }
    }

    private fun extractEtag(response: HttpResponse): String? = response.headers["ETag"]

    private suspend fun extractS3ErrorMessage(response: HttpResponse, fallback: String): String {
        val body = try {
            response.bodyAsText()
        } catch (e: Exception) {
            return fallback
        }
        val match = Regex("<Message>(.*?)</Message>", RegexOption.DOT_MATCHES_ALL).find(body)
        return match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    }
}
