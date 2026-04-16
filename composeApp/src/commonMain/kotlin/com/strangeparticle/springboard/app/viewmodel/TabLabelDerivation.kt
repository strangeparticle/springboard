package com.strangeparticle.springboard.app.viewmodel

const val TAB_LABEL_MAX_LENGTH = 20
const val DEFAULT_EMPTY_TAB_LABEL = "New Tab"

fun deriveTabLabel(source: String?): String {
    if (source.isNullOrBlank()) return DEFAULT_EMPTY_TAB_LABEL
    val rawName = extractBaseName(source)
    if (rawName.isEmpty()) return DEFAULT_EMPTY_TAB_LABEL
    return truncateWithEllipsis(rawName, TAB_LABEL_MAX_LENGTH)
}

private fun extractBaseName(source: String): String {
    val isUrl = source.startsWith("http://") || source.startsWith("https://")
    return if (isUrl) extractUrlBaseName(source) else extractFilePathBaseName(source)
}

private fun extractFilePathBaseName(path: String): String {
    val lastSegment = path.substringAfterLast('/', path)
    return lastSegment.substringBeforeLast('.', lastSegment)
}

private fun extractUrlBaseName(url: String): String {
    val afterScheme = url.substringAfter("://")
    val host = afterScheme.substringBefore('/')
    val pathPart = afterScheme.substringAfter('/', missingDelimiterValue = "").trimEnd('/')
    if (pathPart.isEmpty()) return host
    val lastSegment = pathPart.substringAfterLast('/')
    return lastSegment.substringBeforeLast('.', lastSegment)
}

private fun truncateWithEllipsis(input: String, maxLength: Int): String {
    if (input.length <= maxLength) return input
    return input.take(maxLength - 1) + "…"
}
