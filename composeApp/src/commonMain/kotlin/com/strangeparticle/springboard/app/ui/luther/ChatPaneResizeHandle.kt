package com.strangeparticle.springboard.app.ui.luther

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.gridnav.GridNavSizingConstants
import com.strangeparticle.springboard.app.ui.gridnav.gridHeaderVerticalResizePointerIcon

private const val CHAT_PANE_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION = "Chat pane resize drag handle"

/**
 * Horizontal boundary rendered above the AI chat pane. The decorative line spans the full
 * width; only the centered grip thumb accepts pointer input. Vertical drag motion is
 * reported via [onDragDelta] in pixels (caller converts to dp using its own density).
 *
 * Styling — including the [Icons.Default.DragHandle] glyph, thumb dimensions, divider
 * thickness, and vertical-resize hover cursor — mirrors
 * [com.strangeparticle.springboard.app.ui.gridnav.GridNavHeaderResizeBoundary] so resize
 * affordances look the same throughout the app.
 */
@Composable
internal fun ChatPaneResizeHandle(
    onDragDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline,
            thickness = GridNavSizingConstants.HeaderResizeBoundaryThickness,
        )
        Box(
            modifier = Modifier
                .size(
                    width = GridNavSizingConstants.HeaderResizeThumbWidth,
                    height = GridNavSizingConstants.HeaderResizeThumbHeight,
                )
                .testTag(TestTags.AI_CHAT_RESIZE_HANDLE)
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
                contentDescription = CHAT_PANE_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(
                    width = GridNavSizingConstants.HeaderResizeGripWidth,
                    height = GridNavSizingConstants.HeaderResizeGripHeight,
                ),
            )
        }
    }
}
