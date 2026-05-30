package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.strangeparticle.springboard.app.ui.TestTags

private const val COLUMN_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION = "Resource column resize drag handle"

/**
 * Vertical boundary line drawn between resource labels and data columns. The line itself is
 * decorative; only the centered grip thumb accepts pointer input. Horizontal drag motion
 * on the thumb is reported via [onDragDelta] in pixels (the caller converts to dp).
 */
@Composable
fun GridNavColumnResizeBoundary(
    height: Dp,
    onDragDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Mirror the header resize boundary: the drag gesture runs in a long-lived coroutine
    // keyed on Unit, so route deltas through the latest onDragDelta via rememberUpdatedState.
    // The current resourceLabelWidth state is not recreated today, but this keeps the two
    // resize boundaries consistent and safe against future state-recreation.
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)

    Box(
        modifier = modifier
            .width(GridNavSizingConstants.ColumnResizeThumbWidth)
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = GridNavSizingConstants.ColumnResizeBoundaryThickness,
        )

        Box(
            modifier = Modifier
                .size(
                    width = GridNavSizingConstants.ColumnResizeThumbWidth,
                    height = GridNavSizingConstants.ColumnResizeThumbHeight,
                )
                .testTag(TestTags.GRID_COLUMN_RESIZE_THUMB)
                .pointerHoverIcon(gridColumnHorizontalResizePointerIcon)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        currentOnDragDelta(dragAmount.x)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = COLUMN_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(
                        width = GridNavSizingConstants.ColumnResizeGripWidth,
                        height = GridNavSizingConstants.ColumnResizeGripHeight,
                    )
                    .rotate(90f)
                    .testTag(TestTags.GRID_COLUMN_RESIZE_GRIP_GLYPH),
            )
        }
    }
}
