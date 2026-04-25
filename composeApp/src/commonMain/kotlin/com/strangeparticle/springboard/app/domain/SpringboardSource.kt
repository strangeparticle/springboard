package com.strangeparticle.springboard.app.domain

sealed class SpringboardSource {
    data class HttpSource(val url: String) : SpringboardSource()
    data class S3Source(val bucket: String, val key: String) : SpringboardSource()
    data class FileSource(val path: String) : SpringboardSource()
}

fun determineSpringboardSource(rawSource: String): SpringboardSource {
    val lowercase = rawSource.lowercase()
    return when {
        lowercase.startsWith("https://") || lowercase.startsWith("http://") ->
            SpringboardSource.HttpSource(rawSource)
        lowercase.startsWith("s3://") ->
            parseS3Source(rawSource)
        lowercase.startsWith("file:///") ->
            SpringboardSource.FileSource(rawSource.removePrefix("file://"))
        lowercase.startsWith("file://") ->
            SpringboardSource.FileSource(rawSource.removePrefix("file://"))
        else ->
            SpringboardSource.FileSource(rawSource)
    }
}

private fun parseS3Source(rawSource: String): SpringboardSource.S3Source {
    val afterScheme = rawSource.substring("s3://".length)
    val slashIndex = afterScheme.indexOf('/')
    if (slashIndex < 0) {
        throw IllegalArgumentException("Invalid s3 URL (missing key): $rawSource")
    }
    val bucket = afterScheme.substring(0, slashIndex)
    val key = afterScheme.substring(slashIndex + 1)
    if (bucket.isEmpty()) {
        throw IllegalArgumentException("Invalid s3 URL (empty bucket): $rawSource")
    }
    if (key.isEmpty()) {
        throw IllegalArgumentException("Invalid s3 URL (empty key): $rawSource")
    }
    return SpringboardSource.S3Source(bucket, key)
}
