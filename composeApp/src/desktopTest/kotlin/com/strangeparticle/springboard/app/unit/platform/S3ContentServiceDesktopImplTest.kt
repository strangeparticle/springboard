package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.aws.AwsCredentialProvider
import com.strangeparticle.springboard.app.aws.AwsCredentials
import com.strangeparticle.springboard.app.platform.S3ContentServiceDesktopImpl
import com.strangeparticle.springboard.app.platform.S3GetResult
import com.strangeparticle.springboard.app.platform.S3PutResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class S3ContentServiceDesktopImplTest {

    private val credentials = AwsCredentials(
        accessKeyId = "AKIA",
        secretAccessKey = "secret",
        sessionToken = "session",
        region = "us-east-1",
        expiration = null,
    )

    private fun staticProvider(creds: AwsCredentials? = credentials) = object : AwsCredentialProvider {
        override suspend fun resolve(profile: String): AwsCredentials? = creds
        override fun invalidate(profile: String) = Unit
    }

    @Test
    fun `getObject returns Success with body and etag on 200`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "the body",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ETag, "\"etag-xyz\""),
            )
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.getObject("https://b.s3.us-east-1.amazonaws.com/k", "p")
        assertTrue(result is S3GetResult.Success)
        assertEquals("the body", (result as S3GetResult.Success).content)
        assertEquals("\"etag-xyz\"", result.etag)
    }

    @Test
    fun `getObject maps 403 to Denied with parsed S3 error message`() = runTest {
        val engine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.Forbidden,
                content = "<Error><Code>AccessDenied</Code><Message>Access Denied</Message></Error>",
            )
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.getObject("https://b.s3.us-east-1.amazonaws.com/k", "p")
        assertTrue(result is S3GetResult.Denied)
        assertEquals("Access Denied", (result as S3GetResult.Denied).message)
    }

    @Test
    fun `getObject maps 404 to Failed`() = runTest {
        val engine = MockEngine { _ ->
            respondError(status = HttpStatusCode.NotFound)
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.getObject("https://b.s3.us-east-1.amazonaws.com/k", "p")
        assertTrue(result is S3GetResult.Failed)
        assertEquals("Object not found", (result as S3GetResult.Failed).message)
    }

    @Test
    fun `null credentials map to CredentialsUnavailable`() = runTest {
        val engine = MockEngine { _ -> respond("", HttpStatusCode.OK) }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider(creds = null))
        val getResult = service.getObject("https://b.s3.us-east-1.amazonaws.com/k", "p")
        assertTrue(getResult is S3GetResult.CredentialsUnavailable)
        val putResult = service.putObject("https://b.s3.us-east-1.amazonaws.com/k", "p", "{}", null)
        assertTrue(putResult is S3PutResult.CredentialsUnavailable)
    }

    @Test
    fun `non-virtual-hosted url fails fast without an HTTP call`() = runTest {
        var called = false
        val engine = MockEngine { _ ->
            called = true
            respond("", HttpStatusCode.OK)
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.getObject("https://example.com/file.json", "p")
        assertTrue(result is S3GetResult.Failed)
        assertEquals(false, called)
    }

    @Test
    fun `putObject sends If-Match header when ifMatch is provided`() = runTest {
        var capturedIfMatch: String? = null
        var capturedMethod: HttpMethod? = null
        val engine = MockEngine { request ->
            capturedMethod = request.method
            capturedIfMatch = request.headers["If-Match"]
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ETag, "\"new-etag\""),
            )
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.putObject(
            url = "https://b.s3.us-east-1.amazonaws.com/k",
            profile = "p",
            content = "payload",
            ifMatch = "\"prior\"",
        )
        assertTrue(result is S3PutResult.Success)
        assertEquals("\"new-etag\"", (result as S3PutResult.Success).etag)
        assertEquals(HttpMethod.Put, capturedMethod)
        assertEquals("\"prior\"", capturedIfMatch)
    }

    @Test
    fun `putObject omits If-Match when ifMatch is null`() = runTest {
        var capturedIfMatch: String? = null
        val engine = MockEngine { request ->
            capturedIfMatch = request.headers["If-Match"]
            respond("", HttpStatusCode.OK)
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        service.putObject("https://b.s3.us-east-1.amazonaws.com/k", "p", "payload", ifMatch = null)
        assertNull(capturedIfMatch)
    }

    @Test
    fun `putObject maps 412 to Conflict`() = runTest {
        val engine = MockEngine { _ ->
            respondError(status = HttpStatusCode.PreconditionFailed)
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.putObject("https://b.s3.us-east-1.amazonaws.com/k", "p", "x", ifMatch = "etag")
        assertTrue(result is S3PutResult.Conflict)
    }

    @Test
    fun `putObject maps 403 to Denied`() = runTest {
        val engine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.Forbidden,
                content = "<Error><Code>AccessDenied</Code><Message>Denied for write</Message></Error>",
            )
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        val result = service.putObject("https://b.s3.us-east-1.amazonaws.com/k", "p", "x", ifMatch = null)
        assertTrue(result is S3PutResult.Denied)
        assertEquals("Denied for write", (result as S3PutResult.Denied).message)
    }

    @Test
    fun `Authorization header is set on signed requests`() = runTest {
        var capturedAuth: String? = null
        val engine = MockEngine { request ->
            capturedAuth = request.headers["Authorization"]
            respond("", HttpStatusCode.OK)
        }
        val service = S3ContentServiceDesktopImpl(HttpClient(engine), staticProvider())
        service.getObject("https://b.s3.us-east-1.amazonaws.com/k", "p")
        assertTrue(capturedAuth?.startsWith("AWS4-HMAC-SHA256 Credential=AKIA/") == true)
    }
}
