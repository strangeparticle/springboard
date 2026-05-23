package com.strangeparticle.springboard.app.aws

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.net.URI
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Hand-rolled AWS Signature Version 4 signer. Returns the headers the caller
 * must apply to the outgoing HTTP request (Authorization, x-amz-date,
 * x-amz-content-sha256, and x-amz-security-token when temporary credentials
 * carry a session token).
 *
 * The signer is service-agnostic in its public shape (caller passes the
 * service name, e.g. "s3"), so the same code can later sign Bedrock requests
 * without modification.
 *
 * S3-specific canonicalisation rules in effect here:
 *   - Path segments are URI-encoded once (not twice). Slashes are preserved.
 *   - x-amz-content-sha256 carries the hex SHA-256 of the body and is signed.
 *   - host, x-amz-content-sha256, x-amz-date, and any caller-supplied headers
 *     (e.g. If-Match) are all signed so they cannot be tampered with.
 */
object AwsSigV4Signer {

    fun sign(
        method: String,
        url: String,
        additionalSignedHeaders: Map<String, String>,
        bodyBytes: ByteArray,
        credentials: AwsCredentials,
        service: String,
        signingInstant: Instant,
    ): Map<String, String> {
        val uri = URI(url)
        val host = uri.host ?: error("URL has no host: $url")
        val canonicalPath = canonicalizePath(uri.rawPath ?: "/")
        val canonicalQuery = canonicalizeQuery(uri.rawQuery)
        val payloadHash = hexSha256(bodyBytes)

        val timestamp = formatAmzTimestamp(signingInstant)
        val dateStamp = timestamp.substring(0, 8)

        val headersToSign = sortedMapOf<String, String>(compareBy { it.lowercase() })
        headersToSign["host"] = host
        headersToSign["x-amz-content-sha256"] = payloadHash
        headersToSign["x-amz-date"] = timestamp
        credentials.sessionToken?.let { headersToSign["x-amz-security-token"] = it }
        additionalSignedHeaders.forEach { (name, value) ->
            headersToSign[name.lowercase()] = value.trim()
        }

        val canonicalHeaders = headersToSign.entries.joinToString(separator = "") { (name, value) ->
            "$name:${collapseWhitespace(value)}\n"
        }
        val signedHeaderNames = headersToSign.keys.joinToString(separator = ";")

        val canonicalRequest = buildString {
            append(method.uppercase()).append('\n')
            append(canonicalPath).append('\n')
            append(canonicalQuery).append('\n')
            append(canonicalHeaders).append('\n')
            append(signedHeaderNames).append('\n')
            append(payloadHash)
        }

        val credentialScope = "$dateStamp/${credentials.region}/$service/aws4_request"
        val stringToSign = buildString {
            append("AWS4-HMAC-SHA256\n")
            append(timestamp).append('\n')
            append(credentialScope).append('\n')
            append(hexSha256(canonicalRequest.toByteArray(Charsets.UTF_8)))
        }

        val signingKey = deriveSigningKey(credentials.secretAccessKey, dateStamp, credentials.region, service)
        val signature = hex(hmacSha256(signingKey, stringToSign.toByteArray(Charsets.UTF_8)))

        val authorization = "AWS4-HMAC-SHA256 " +
            "Credential=${credentials.accessKeyId}/$credentialScope, " +
            "SignedHeaders=$signedHeaderNames, " +
            "Signature=$signature"

        return buildMap {
            put("Authorization", authorization)
            put("x-amz-date", timestamp)
            put("x-amz-content-sha256", payloadHash)
            credentials.sessionToken?.let { put("x-amz-security-token", it) }
        }
    }

    private fun canonicalizePath(rawPath: String): String {
        if (rawPath.isEmpty()) return "/"
        // S3 wants each segment URI-encoded once; slashes preserved. The raw
        // path from java.net.URI is already encoded by the URI parser, but
        // S3 uses a stricter set of unreserved characters than RFC 3986, so we
        // re-encode segment by segment to be safe.
        return rawPath.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) "" else uriEncode(segment, encodeSlash = false)
        }
    }

    private fun canonicalizeQuery(rawQuery: String?): String {
        if (rawQuery.isNullOrEmpty()) return ""
        val pairs = rawQuery.split('&').map { pair ->
            val equalsIndex = pair.indexOf('=')
            if (equalsIndex < 0) {
                uriEncode(pair, encodeSlash = true) to ""
            } else {
                uriEncode(pair.substring(0, equalsIndex), encodeSlash = true) to
                    uriEncode(pair.substring(equalsIndex + 1), encodeSlash = true)
            }
        }
        return pairs
            .sortedWith(compareBy({ it.first }, { it.second }))
            .joinToString("&") { "${it.first}=${it.second}" }
    }

    private fun uriEncode(input: String, encodeSlash: Boolean): String {
        val builder = StringBuilder(input.length)
        val bytes = input.toByteArray(Charsets.UTF_8)
        for (byte in bytes) {
            val intValue = byte.toInt() and 0xFF
            val char = intValue.toChar()
            val isUnreserved = (char in 'A'..'Z') ||
                (char in 'a'..'z') ||
                (char in '0'..'9') ||
                char == '_' || char == '-' || char == '~' || char == '.'
            if (isUnreserved || (!encodeSlash && char == '/')) {
                builder.append(char)
            } else {
                builder.append('%')
                builder.append("%02X".format(intValue))
            }
        }
        return builder.toString()
    }

    private fun collapseWhitespace(value: String): String {
        return value.trim().replace(Regex("\\s+"), " ")
    }

    private fun deriveSigningKey(secret: String, dateStamp: String, region: String, service: String): ByteArray {
        val kSecret = ("AWS4$secret").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp.toByteArray(Charsets.UTF_8))
        val kRegion = hmacSha256(kDate, region.toByteArray(Charsets.UTF_8))
        val kService = hmacSha256(kRegion, service.toByteArray(Charsets.UTF_8))
        return hmacSha256(kService, "aws4_request".toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hexSha256(data: ByteArray): String =
        hex(MessageDigest.getInstance("SHA-256").digest(data))

    private fun hex(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val intValue = byte.toInt() and 0xFF
            builder.append(HEX_DIGITS[intValue ushr 4])
            builder.append(HEX_DIGITS[intValue and 0x0F])
        }
        return builder.toString()
    }

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()

    private fun formatAmzTimestamp(instant: Instant): String {
        val utc = instant.toLocalDateTime(TimeZone.UTC)
        return "%04d%02d%02dT%02d%02d%02dZ".format(
            utc.year,
            utc.monthNumber,
            utc.dayOfMonth,
            utc.hour,
            utc.minute,
            utc.second,
        )
    }
}
