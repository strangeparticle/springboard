package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.model.AppColumn
import com.strangeparticle.springboard.app.domain.model.AppColumnSlot
import com.strangeparticle.springboard.app.domain.model.AppGroupColumnSpan
import com.strangeparticle.springboard.app.domain.model.containsSlotIndex
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

data class GridNavGroupLabelHighlight(
    val startSlotIndex: Int,
    val columnCount: Int,
)

fun resolveGroupLabelHighlight(
    slots: List<AppColumnSlot>,
    groupSpans: List<AppGroupColumnSpan>,
    hoveredAppId: String?,
    hoveredHeaderAppId: String?,
): GridNavGroupLabelHighlight? {
    val appId = hoveredAppId ?: hoveredHeaderAppId ?: return null
    val slotIndex = slots.indexOfFirst { it is AppColumn && it.app.id == appId }
    if (slotIndex < 0) return null

    val groupSpan = groupSpans.firstOrNull { it.containsSlotIndex(slotIndex) }
    return if (groupSpan != null) {
        GridNavGroupLabelHighlight(groupSpan.startSlotIndex, groupSpan.columnCount)
    } else {
        GridNavGroupLabelHighlight(slotIndex, columnCount = 1)
    }
}

fun appIdAtGroupLabelStripPointer(
    x: Float,
    columnWidthPx: Float,
    slots: List<AppColumnSlot>,
): String? {
    if (x < 0f) return null
    val slotIndex = (x / columnWidthPx).toInt()
    val slot = slots.getOrNull(slotIndex)
    return if (slot is AppColumn) slot.app.id else null
}

@Composable
fun GridNavGroupLabelStrip(
    slots: List<AppColumnSlot>,
    groupSpans: List<AppGroupColumnSpan>,
    resourceLabelWidth: Dp,
    gridHeaderHeight: Dp,
    groupLabelStripHeight: Dp,
    hoveredAppId: String?,
    hoveredHeaderAppId: String?,
    onHoveredAppChange: (String?) -> Unit,
    onColumnClick: (String) -> Unit,
) {
    val highlight = resolveGroupLabelHighlight(slots, groupSpans, hoveredAppId, hoveredHeaderAppId)
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .offset(x = resourceLabelWidth, y = gridHeaderHeight - groupLabelStripHeight)
            .width(CommonUiConstants.GridColumnWidth * slots.size)
            .height(groupLabelStripHeight)
            .pointerInput(slots) {
                val columnWidthPx = CommonUiConstants.GridColumnWidth.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                onHoveredAppChange(
                                    position?.let { appIdAtGroupLabelStripPointer(it.x, columnWidthPx, slots) }
                                )
                            }
                            PointerEventType.Exit -> onHoveredAppChange(null)
                            PointerEventType.Press -> {
                                if (position != null) {
                                    val appId = appIdAtGroupLabelStripPointer(position.x, columnWidthPx, slots)
                                    if (appId != null) {
                                        onColumnClick(appId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (highlight != null) {
            Box(
                modifier = Modifier
                    .offset(x = CommonUiConstants.GridColumnWidth * highlight.startSlotIndex)
                    .width(CommonUiConstants.GridColumnWidth * highlight.columnCount)
                    .fillMaxSize()
                    .background(highlightColor)
            )
        }

        groupSpans.forEach { span ->
            Box(
                modifier = Modifier
                    .offset(x = CommonUiConstants.GridColumnWidth * span.startSlotIndex)
                    .width(CommonUiConstants.GridColumnWidth * span.columnCount)
                    .height(groupLabelStripHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = span.description,
                    color = labelColor,
                    style = GroupLabelTextStyle,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset(y = (-1).dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}
