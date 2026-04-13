package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.App

/** Text style for the app name line in column headers. */
val HeaderNameTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.Bold,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Text style for the app id line in column headers. */
val HeaderIdTextStyle = TextStyle(
    fontSize = GridNavHeaderIdTextSizeSp.sp,
    lineHeight = GridNavHeaderIdTextSizeSp.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/**
 * Holds pre-computed header sizing derived from the app list and font metrics.
 * Created via [rememberGridNavHeaderSizing].
 */
class GridNavHeaderSizing(
    val initialHeaderHeight: Dp,
    val stackedTextHeightPx: Float,
    val textMeasurer: TextMeasurer,
    val density: Density,
)

/**
 * Measures font metrics and computes the initial (clamped) header height for the
 * given app list. The result is remembered and only recomputed when the app list changes.
 */
@Composable
fun rememberGridNavHeaderSizing(apps: List<App>): GridNavHeaderSizing {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val stackedTextHeightPx = remember(HeaderNameTextStyle, HeaderIdTextStyle) {
        val nameHeight = textMeasurer.measure("Mg", HeaderNameTextStyle).size.height
        val idHeight = textMeasurer.measure("MG", HeaderIdTextStyle).size.height
        val spacerPx = with(density) { 2.dp.toPx() }
        nameHeight + spacerPx + idHeight
    }

    val initialHeaderHeight = remember(apps, stackedTextHeightPx) {
        val longestTruncatedName = apps
            .maxByOrNull { it.name.length }
            ?.name
            ?.let(::truncateHeaderText)
            ?: ""
        val measuredWidth = textMeasurer.measure(longestTruncatedName, HeaderNameTextStyle).size.width
        val computedHeight = with(density) {
            val rotatedHeightPx = computeRotatedHeaderHeightPx(
                measuredWidth.toFloat(), stackedTextHeightPx
            )
            rotatedHeightPx.toDp() + HeaderRotationVerticalPadding
        }
        computedHeight.coerceIn(
            GridNavSizingConstants.MinHeaderHeight,
            GridNavSizingConstants.MaxHeaderHeight,
        )
    }

    return remember(initialHeaderHeight, stackedTextHeightPx) {
        GridNavHeaderSizing(initialHeaderHeight, stackedTextHeightPx, textMeasurer, density)
    }
}

/**
 * Computes the visible (possibly truncated) header name for each app at the given
 * header height. Pure function — the composable caller wraps this in `remember`.
 */
fun computeVisibleHeaderNames(
    apps: List<App>,
    gridHeaderHeight: Dp,
    stackedTextHeightPx: Float,
    textMeasurer: TextMeasurer,
    density: Density,
): Map<String, String> {
    val availableRotatedHeightPx = with(density) {
        (gridHeaderHeight - HeaderRotationVerticalPadding).toPx().coerceAtLeast(0f)
    }
    val maxNameWidthPx = (availableRotatedHeightPx / Sin45) - stackedTextHeightPx
    return apps.associate { app ->
        app.id to truncateHeaderTextToFitWidth(
            text = app.name,
            maxWidthPx = maxNameWidthPx,
            textMeasurer = textMeasurer,
            style = HeaderNameTextStyle,
        )
    }
}
