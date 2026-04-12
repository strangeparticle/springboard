package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Applies zoom scaling to a composable. Two mechanisms work together:
 *
 * - The [layout] modifier divides incoming constraints by [scale] so the content is
 *   measured at its natural (unscaled) size, then reports the scaled dimensions back to
 *   the parent. This keeps scroll bounds correct — the scrollable container sees the
 *   zoomed size and provides appropriate scroll range.
 *
 * - The [graphicsLayer] applies the visual scale transformation anchored at top-left,
 *   so the content draws at the zoomed size without re-layout.
 */
fun Modifier.gridZoomScale(scale: Float): Modifier =
    this
        .layout { measurable, constraints ->
            val scaledConstraints = if (scale > 0f) {
                constraints.copy(
                    maxWidth = if (constraints.maxWidth == Constraints.Infinity) {
                        Constraints.Infinity
                    } else {
                        (constraints.maxWidth / scale).roundToInt()
                    },
                    maxHeight = if (constraints.maxHeight == Constraints.Infinity) {
                        Constraints.Infinity
                    } else {
                        (constraints.maxHeight / scale).roundToInt()
                    },
                )
            } else {
                constraints
            }
            val placeable = measurable.measure(scaledConstraints)
            layout(
                (placeable.width * scale).roundToInt(),
                (placeable.height * scale).roundToInt(),
            ) {
                placeable.place(0, 0)
            }
        }
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            transformOrigin = TransformOrigin(0f, 0f),
        )
