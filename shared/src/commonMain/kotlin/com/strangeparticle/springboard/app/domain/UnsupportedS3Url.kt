package com.strangeparticle.springboard.app.domain

const val UnsupportedS3UrlMessage =
    "Please use the full HTTPS URL for S3-hosted files. The s3:// notation is not supported because it lacks required region and endpoint information."

fun isUnsupportedS3Url(rawSource: String): Boolean =
    rawSource.trimStart().startsWith("s3://", ignoreCase = true)
