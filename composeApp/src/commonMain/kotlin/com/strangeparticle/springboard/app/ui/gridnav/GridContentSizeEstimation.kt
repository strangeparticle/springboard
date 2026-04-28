package com.strangeparticle.springboard.app.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.allEnvsResources
import com.strangeparticle.springboard.app.domain.model.envSpecificResources
import com.strangeparticle.springboard.app.domain.model.hasAnyAllEnvsActivators
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import kotlin.math.min

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
 * Estimates the natural grid content height in dp: rotated-header slot + data rows
 * across all rendered sections + per-section heading and spacer rows + dividers +
 * content padding.
 *
 * When both the all-envs and env-specific sections render, the env section
 * contributes two extra GridRowHeight rows (one inter-section spacer + one heading
 * row) that the rotated header slot does not absorb.
 */
fun estimateGridContentHeightDp(springboard: Springboard): Int {
    val headerHeightDp = estimateHeaderHeightDp(springboard)
    val gridRowHeightDp = CommonUiConstants.GridRowHeight.value

    val hasAllEnvsSection = springboard.hasAnyAllEnvsActivators()
    val allEnvsRowCount = if (hasAllEnvsSection) springboard.allEnvsResources().size else 0
    val envRowCount = springboard.envSpecificResources().size
    val totalRowCount = allEnvsRowCount + envRowCount

    val sectionCount = (if (hasAllEnvsSection) 1 else 0) + 1
    val interSectionRowCount = (sectionCount - 1) * 2  // spacer + heading per extra section

    val dividersTotalDp = totalRowCount * 0.5f

    val gridContentHeight = headerHeightDp +
        ((totalRowCount + interSectionRowCount) * gridRowHeightDp) +
        dividersTotalDp +
        GridContentPaddingDp

    return gridContentHeight.toInt()
}

// Activator preview: 11sp text + 2dp vertical padding on each side ≈ 19dp
const val ActivatorPreviewHeightDp = 19

/**
 * Subtracts UI chrome heights (navbar, status bar, activator preview) from a viewport
 * to determine the space available for grid content. Returns (availableWidth, availableHeight).
 */
fun computeAvailableGridArea(viewportWidthDp: Int, viewportHeightDp: Int): Pair<Int, Int> {
    val navbarHeightDp = CommonUiConstants.NavbarHeight.value.toInt()
    val statusBarHeightDp = CommonUiConstants.StatusBarHeight.value.toInt()
    val availableHeight = viewportHeightDp - navbarHeightDp - statusBarHeightDp - ActivatorPreviewHeightDp
    return viewportWidthDp to availableHeight
}

/**
 * Computes a conservative zoom preset that fits the springboard content within the
 * given available area. Pure function — no platform dependencies.
 */
fun computeZoomToFit(
    availableWidthDp: Int,
    availableHeightDp: Int,
    springboard: Springboard,
): GridZoomSelection {
    val naturalWidth = estimateGridContentWidthDp(springboard)
    val naturalHeight = estimateGridContentHeightDp(springboard)

    if (naturalWidth <= 0 || naturalHeight <= 0) return GridZoomSelection.FixedZoom(100)

    val fitPercent = min(
        availableWidthDp * 100 / naturalWidth,
        availableHeightDp * 100 / naturalHeight,
    )
    return GridZoomSelection.conservativePresetFor(fitPercent)
}
