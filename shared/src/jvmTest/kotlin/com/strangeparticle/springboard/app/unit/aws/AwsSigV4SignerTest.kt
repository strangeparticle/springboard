package com.strangeparticle.springboard.app.unit.aws

import com.strangeparticle.springboard.app.aws.AwsCredentials
import com.strangeparticle.springboard.app.aws.AwsSigV4Signer
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AwsSigV4SignerTest {

    /**
     * Static long-lived credentials with the well-known AWS docs example
     * access/secret pair used throughout the SigV4 documentation.
     */
    private val docsCredentials = AwsCredentials(
        accessKeyId = "AKIAIOSFODNN7EXAMPLE",
        secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
        sessionToken = null,
        region = "us-east-1",
        expiration = null,
    )

    private val signingTime = Instant.parse("2024-01-15T12:34:56Z")

    @Test
    fun `GET request produces deterministic Authorization header for empty body`() {
        val headers = AwsSigV4Signer.sign(
            method = "GET",
            url = "https://my-bucket.s3.us-east-1.amazonaws.com/path/to/file.json",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = ByteArray(0),
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        assertEquals("20240115T123456Z", headers["x-amz-date"])
        // Empty body has SHA256 = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            headers["x-amz-content-sha256"],
        )
        val authorization = headers["Authorization"]!!
        assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240115/us-east-1/s3/aws4_request, "))
        assertTrue(authorization.contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date, "))
        assertTrue(authorization.contains("Signature="))
        // The signature is 64 hex chars; no whitespace.
        val signature = authorization.substringAfter("Signature=")
        assertEquals(64, signature.length)
        assertTrue(signature.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `PUT request signs the body and includes If-Match in canonical headers`() {
        val body = "{\"name\":\"sb\"}".toByteArray(Charsets.UTF_8)
        val withoutIfMatch = AwsSigV4Signer.sign(
            method = "PUT",
            url = "https://b.s3.us-east-1.amazonaws.com/sb.json",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = body,
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        val withIfMatch = AwsSigV4Signer.sign(
            method = "PUT",
            url = "https://b.s3.us-east-1.amazonaws.com/sb.json",
            additionalSignedHeaders = mapOf("If-Match" to "\"abc123\""),
            bodyBytes = body,
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )

        val sigWithout = withoutIfMatch["Authorization"]!!.substringAfter("Signature=")
        val sigWith = withIfMatch["Authorization"]!!.substringAfter("Signature=")
        assertTrue(sigWithout != sigWith, "Adding If-Match must change the signature")
        assertTrue(withIfMatch["Authorization"]!!.contains(";if-match;"))
    }

    @Test
    fun `omits security token header when credentials have no session token`() {
        val headers = AwsSigV4Signer.sign(
            method = "GET",
            url = "https://b.s3.us-east-1.amazonaws.com/k",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = ByteArray(0),
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        assertNull(headers["x-amz-security-token"])
        assertTrue(!headers["Authorization"]!!.contains("x-amz-security-token"))
    }

    @Test
    fun `includes security token header and signs it when session token is present`() {
        val temporary = docsCredentials.copy(sessionToken = "session-token-xyz")
        val headers = AwsSigV4Signer.sign(
            method = "GET",
            url = "https://b.s3.us-east-1.amazonaws.com/k",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = ByteArray(0),
            credentials = temporary,
            service = "s3",
            signingInstant = signingTime,
        )
        assertEquals("session-token-xyz", headers["x-amz-security-token"])
        assertTrue(headers["Authorization"]!!.contains(";x-amz-security-token"))
    }

    @Test
    fun `payload changes produce different signatures`() {
        val a = AwsSigV4Signer.sign(
            method = "PUT",
            url = "https://b.s3.us-east-1.amazonaws.com/k",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = "hello".toByteArray(Charsets.UTF_8),
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        val b = AwsSigV4Signer.sign(
            method = "PUT",
            url = "https://b.s3.us-east-1.amazonaws.com/k",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = "world".toByteArray(Charsets.UTF_8),
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        assertTrue(a["Authorization"] != b["Authorization"])
        assertTrue(a["x-amz-content-sha256"] != b["x-amz-content-sha256"])
    }

    @Test
    fun `path segments with special characters are encoded`() {
        val headers = AwsSigV4Signer.sign(
            method = "GET",
            url = "https://b.s3.us-east-1.amazonaws.com/folder/file%20with%20spaces.json",
            additionalSignedHeaders = emptyMap(),
            bodyBytes = ByteArray(0),
            credentials = docsCredentials,
            service = "s3",
            signingInstant = signingTime,
        )
        // Just exercise the code path; assert a signature was produced.
        assertEquals(64, headers["Authorization"]!!.substringAfter("Signature=").length)
    }
}
