package com.strangeparticle.springboard.app.domain

sealed class SpringboardSource {
    data class HttpSource(val url: String) : SpringboardSource()
    data class FileSource(val path: String) : SpringboardSource()
}

fun determineSpringboardSource(rawSource: String): SpringboardSource {
    val lowercase = rawSource.lowercase()
    return when {
        isUnsupportedS3Url(rawSource) ->
            throw IllegalArgumentException(UnsupportedS3UrlMessage)
        lowercase.startsWith("https://") || lowercase.startsWith("http://") ->
            SpringboardSource.HttpSource(rawSource)
        lowercase.startsWith("file:///") ->
            SpringboardSource.FileSource(rawSource.removePrefix("file://"))
        lowercase.startsWith("file://") ->
            SpringboardSource.FileSource(rawSource.removePrefix("file://"))
        else ->
            SpringboardSource.FileSource(rawSource)
    }
}
