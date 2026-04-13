package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Renders a single rotated column header with parallelogram highlight.
 * The header contains a two-line text stack (app name + app id) rotated -45 degrees,
 * positioned so the bottom-left corner of the text aligns with the column's data cells.
 */
@Composable
fun GridNavColumnHeader(
    appId: String,
    displayName: String,
    gridHeaderHeight: Dp,
    isHeaderHighlighted: Boolean,
    isHeaderHovered: Boolean,
) {
    val headerHighlightColor = if (isHeaderHovered)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeaderHeight)
            .zIndex(if (isHeaderHighlighted) 1f else 0f)
            .graphicsLayer { clip = false }
            .drawBehind {
                if (isHeaderHighlighted) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width, size.height)
                        lineTo(size.width + size.height, 0f)
                        lineTo(size.height, 0f)
                        close()
                    }
                    drawPath(path, color = headerHighlightColor)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(maxWidth = 10_000, maxHeight = 10_000)
                    )
                    val rotatedBottomY = (placeable.width + placeable.height) * Sin45 / 2f
                    val uniformShift = placeable.height * Sin45 / 4f
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            x = (constraints.maxWidth / 2f + rotatedBottomY - placeable.width / 2f - uniformShift).toInt(),
                            y = (constraints.maxHeight - placeable.height / 2f - rotatedBottomY + uniformShift).toInt()
                        )
                    }
                }
                .rotate(-45f)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Column {
                Text(
                    text = displayName,
                    style = HeaderNameTextStyle,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Shift the id right by (spacer + id line height) pre-rotation so
                // that after the -45 degree rotation its left-bottom corner lands on the
                // same horizontal line as the name's left-bottom corner.
                Text(
                    text = appId.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = HeaderIdTextStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = RotatedHeaderIdTextOffset),
                )
            }
        }
    }
}
