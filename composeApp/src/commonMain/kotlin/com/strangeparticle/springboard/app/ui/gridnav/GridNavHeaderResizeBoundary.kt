package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.strangeparticle.springboard.app.ui.TestTags

private const val HEADER_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION = "Header resize drag handle"

/**
 * Horizontal boundary line drawn between the rotated header row and the data row of the
 * grid nav. The line itself is decorative; only the centered grip thumb accepts pointer
 * input. Vertical drag motion on the thumb is reported via [onDragDelta] in pixels
 * (the caller converts to dp using its own density).
 */
@Composable
fun GridNavHeaderResizeBoundary(
    totalGridWidth: Dp,
    onDragDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(totalGridWidth),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline,
            thickness = GridNavSizingConstants.HeaderResizeBoundaryThickness,
        )

        // Drag thumb: only this region accepts pointer input. Wider/taller than the divider
        // line so the user can grab it reliably. Shows a vertical-resize cursor on hover.
        Box(
            modifier = Modifier
                .size(
                    width = GridNavSizingConstants.HeaderResizeThumbWidth,
                    height = GridNavSizingConstants.HeaderResizeThumbHeight,
                )
                .testTag(TestTags.GRID_HEADER_RESIZE_THUMB)
                .pointerHoverIcon(gridHeaderVerticalResizePointerIcon)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = HEADER_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(
                        width = GridNavSizingConstants.HeaderResizeGripWidth,
                        height = GridNavSizingConstants.HeaderResizeGripHeight,
                    )
                    .testTag(TestTags.GRID_HEADER_RESIZE_GRIP_GLYPH),
            )
        }
    }
}
