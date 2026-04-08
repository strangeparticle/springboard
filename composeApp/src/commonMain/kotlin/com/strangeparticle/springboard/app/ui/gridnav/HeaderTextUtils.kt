package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.strangeparticle.springboard.app.domain.model.*

const val MaxHeaderChars = 20
const val HeaderEllipsis = "…"

fun truncateHeaderText(text: String): String =
    if (text.length > MaxHeaderChars) text.take(MaxHeaderChars - 1) + HeaderEllipsis else text

/**
 * Returns the longest prefix of [text] (with a trailing ellipsis when shortened) whose
 * measured pixel width does not exceed [maxWidthPx], when laid out with [style] using
 * [textMeasurer]. Returns the full string when it already fits, and `"…"` when even a
 * single character does not fit.
 *
 * Used by the rotated grid nav column headers to recompute visible text whenever the
 * user resizes the header height — more vertical space converts (via the 45° rotation
 * geometry) into more horizontal text room, which reveals additional characters.
 */
fun truncateHeaderTextToFitWidth(
    text: String,
    maxWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): String = truncateHeaderTextToFitWidth(text, maxWidthPx) { candidate ->
    textMeasurer.measure(candidate, style).size.width.toFloat()
}

/**
 * Pure variant suitable for unit tests — no Compose dependencies. The [measureWidthPx]
 * lambda returns the rendered pixel width of any candidate string under whatever font
 * style the caller has in mind.
 */
internal fun truncateHeaderTextToFitWidth(
    text: String,
    maxWidthPx: Float,
    measureWidthPx: (String) -> Float,
): String {
    if (text.isEmpty()) return text
    if (maxWidthPx <= 0f) return HeaderEllipsis
    if (measureWidthPx(text) <= maxWidthPx) return text

    // Binary search for the largest prefix length whose `prefix + "…"` still fits.
    var low = 0
    var high = text.length - 1
    var bestPrefixLength = 0
    while (low <= high) {
        val mid = (low + high) / 2
        val candidate = text.take(mid) + HeaderEllipsis
        if (measureWidthPx(candidate) <= maxWidthPx) {
            bestPrefixLength = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return if (bestPrefixLength == 0) HeaderEllipsis else text.take(bestPrefixLength) + HeaderEllipsis
}

fun activatorPreviewText(activator: Activator?): String? = when (activator) {
    is UrlActivator -> "url: ${activator.url}"
    is UrlTemplateActivator -> "url: ${activator.urlTemplate}"
    is CommandActivator -> "cmd: ${activator.commandTemplate}"
    null -> null
}
