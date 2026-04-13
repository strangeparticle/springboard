package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.unit.dp

/** Sine of 45°, used throughout rotated header geometry calculations. */
const val Sin45 = 0.7071f

/**
 * Vertical padding added after the rotated header height calculation. Accounts for
 * the gap between the bottom of the rotated text stack and the header/data boundary.
 */
val HeaderRotationVerticalPadding = 12.dp

/**
 * Horizontal offset applied to the app-id text line before rotation. After the -45°
 * rotation this aligns the id's left-bottom corner with the name's left-bottom corner.
 */
val RotatedHeaderIdTextOffset = 14.dp

/** Delay in milliseconds before a guidance tooltip is dismissed after the pointer leaves. */
const val GuidanceDismissDelayMs = 300L

/**
 * Computes the pixel height of a rotated header from the measured text width and stacked
 * text height. Both GridNav (runtime text measurement) and GridContentSizeEstimation
 * (character-width approximation) use this formula so they stay in sync.
 */
fun computeRotatedHeaderHeightPx(textWidthPx: Float, stackedTextHeightPx: Float): Float =
    (textWidthPx + stackedTextHeightPx) * Sin45
