package com.strangeparticle.springboard.app.viewmodel

const val TAB_LABEL_MAX_LENGTH = 20

/**
 * Derives the tab label from a loaded springboard's `name` property. Trims
 * surrounding whitespace and truncates with a trailing ellipsis when the name
 * exceeds [TAB_LABEL_MAX_LENGTH] characters. The springboard JSON schema
 * requires `name`, so this function is only invoked with a non-blank value.
 */
fun deriveTabLabel(springboardName: String): String {
    val trimmed = springboardName.trim()
    return truncateWithEllipsis(trimmed, TAB_LABEL_MAX_LENGTH)
}

private fun truncateWithEllipsis(input: String, maxLength: Int): String {
    if (input.length <= maxLength) return input
    return input.take(maxLength - 1) + "…"
}
