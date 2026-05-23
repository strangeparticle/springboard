package com.strangeparticle.springboard.app.aws

/**
 * Parses and validates S3 HTTPS URLs in the **virtual-hosted** form:
 *
 *     https://<bucket>.s3.<region>.amazonaws.com/<key>
 *
 * Path-style URLs (`https://s3.<region>.amazonaws.com/<bucket>/<key>`) and the
 * legacy region-less form (`https://<bucket>.s3.amazonaws.com/<key>`) are not
 * supported — the dialog rejects them so the region is always explicit in the
 * URL and never inferred from a profile default.
 */
object S3UrlParser {

    private val virtualHostedRegex = Regex(
        "^https://([a-z0-9][a-z0-9.\\-]*)\\.s3\\.([a-z0-9\\-]+)\\.amazonaws\\.com/(.+)$"
    )

    fun isValidVirtualHostedS3Url(rawUrl: String): Boolean =
        virtualHostedRegex.matches(rawUrl.trim())

    /** Region segment of a virtual-hosted S3 URL, or null when the URL is not in that form. */
    fun region(rawUrl: String): String? =
        virtualHostedRegex.matchEntire(rawUrl.trim())?.groupValues?.get(2)
}
