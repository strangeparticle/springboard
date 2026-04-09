package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

/**
 * Transparent overlay that provides parallelogram-shaped hover detection and click handling
 * for the rotated column headers. Sits on top of the entire header area.
 *
 * Why this is necessary: Compose hit-testing is always rectangular — it's based on layout
 * bounds regardless of what is drawn. The parallelogram highlights in [GridNavAppColumn] are
 * painted via `drawBehind` with a [Path], which only affects rendering and has zero effect on
 * pointer event delivery. `graphicsLayer { clip = false }` lets the parallelogram draw outside
 * the column Box bounds into neighboring columns, but pointer events still only fire within
 * each column's rectangular layout bounds.
 *
 * Alternatives considered:
 * - `Modifier.clip()` with a custom Shape would clip the hit area, but also clips the visual
 *   content — so the parallelogram drawing extending into neighbor columns would be cut off.
 * - Per-column `pointerInput` handlers with manual hit-testing don't work for the portion of
 *   the parallelogram that extends beyond that column's rectangular bounds.
 *
 * This overlay avoids both problems: a single transparent rectangle covers the full header
 * region, and the `pointerInput` handler uses a coordinate transform to determine which
 * parallelogram the pointer is inside. The transform — `effectiveX = pointerX + pointerY -
 * headerHeight` — skews the coordinate space so each 45° diagonal strip maps to a simple
 * column-width interval, making column lookup just integer division.
 */
@Composable
fun GridNavAppColumnHeadingHoverDetectionOverlay(
    apps: List<App>,
    gridHeaderHeight: Dp,
    horizontalOffset: Dp,
    onHoveredAppChange: (String?) -> Unit,
    onColumnClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(x = horizontalOffset)
            .width(CommonUiConstants.GridColumnWidth * apps.size + gridHeaderHeight)
            .height(gridHeaderHeight)
            .pointerInput(apps) {
                val columnWidthPx = CommonUiConstants.GridColumnWidth.toPx()

                fun columnIndexAtPointer(x: Float, y: Float): Int? {
                    val effectiveX = x + y - size.height
                    if (effectiveX < 0) return null
                    val index = (effectiveX / columnWidthPx).toInt()
                    return if (index in apps.indices) index else null
                }

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                if (position != null) {
                                    val columnIndex = columnIndexAtPointer(position.x, position.y)
                                    onHoveredAppChange(columnIndex?.let { apps[it].id })
                                }
                            }
                            PointerEventType.Exit -> {
                                onHoveredAppChange(null)
                            }
                            PointerEventType.Press -> {
                                if (position != null) {
                                    val columnIndex = columnIndexAtPointer(position.x, position.y)
                                    if (columnIndex != null) {
                                        onColumnClick(apps[columnIndex].id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    )
}
