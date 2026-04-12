package com.strangeparticle.springboard.app.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

// Character-width estimation for rotated header height. These are rough approximations
// that track GridNav's runtime text measurement closely enough for pre-composition sizing.
private const val MaxHeaderCharCount = 20
private const val HeaderTextAvgCharWidthDp = 7.5f
// Stacked text height: 13sp name + 2dp spacer + 8sp id ≈ 27dp
private const val HeaderTextHeightDp = 27f
// Safety factor covers font-metric differences between this character-width estimate
// and GridNav's actual text measurement (which varies by platform and density).
private const val HeaderEstimationSafetyFactor = 1.15f

// Grid content area padding (matches GridNav's Modifier.padding(16.dp) on each side)
private const val GridContentPaddingDp = 32

/**
 * Estimates the rotated header height in dp from springboard data, without
 * requiring a text measurer or density context.
 */
fun estimateHeaderHeightDp(springboard: Springboard): Int {
    val longestAppNameLength = springboard.apps.maxOfOrNull {
        minOf(it.name.length, MaxHeaderCharCount)
    } ?: 0
    val estimatedHeaderTextWidthDp = longestAppNameLength * HeaderTextAvgCharWidthDp
    val rotatedHeight = (computeRotatedHeaderHeightPx(estimatedHeaderTextWidthDp, HeaderTextHeightDp) *
        HeaderEstimationSafetyFactor).toInt()
    return rotatedHeight + HeaderRotationVerticalPadding.value.toInt()
}

/**
 * Estimates the natural grid content width in dp: resource labels + app columns +
 * header diagonal overhang + content padding.
 */
fun estimateGridContentWidthDp(springboard: Springboard): Int {
    val headerOverhangDp = estimateHeaderHeightDp(springboard)
    val gridColumnWidthDp = CommonUiConstants.GridColumnWidth.value.toInt()
    val resourceLabelWidthDp = CommonUiConstants.ResourceLabelWidth.value.toInt()

    return resourceLabelWidthDp +
        (springboard.apps.size * gridColumnWidthDp) +
        headerOverhangDp +
        GridContentPaddingDp
}

/**
 * Estimates the natural grid content height in dp: header + data rows + dividers +
 * content padding.
 */
fun estimateGridContentHeightDp(springboard: Springboard): Int {
    val headerHeightDp = estimateHeaderHeightDp(springboard)
    val gridRowHeightDp = CommonUiConstants.GridRowHeight.value
    val dividersTotalDp = springboard.resources.size * 0.5f

    val gridContentHeight = headerHeightDp +
        (springboard.resources.size * gridRowHeightDp) +
        dividersTotalDp +
        GridContentPaddingDp

    return gridContentHeight.toInt()
}
