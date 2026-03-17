package com.strangeparticle.springboard.app.ui.guidance

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.strangeparticle.springboard.app.platform.copyToClipboard
import com.strangeparticle.springboard.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A compact, tooltip-style popup that displays guidance lines for a grid cell.
 *
 * Called from within the cell Box in GridNav. Uses Compose Popup anchored to
 * its parent, so no manual coordinate plumbing is needed.
 *
 * @param guidanceLines The lines of guidance text to display.
 * @param onTooltipHoverChanged Reports whether the tooltip surface is currently hovered,
 *        so the parent can keep the tooltip visible for text selection / copy.
 */
@Composable
fun GuidanceTooltip(
    guidanceLines: List<String>,
    onTooltipHoverChanged: (Boolean) -> Unit
) {
    var showCopied by remember { mutableStateOf(false) }
    val copyScope = rememberCoroutineScope()
    val tooltipInteractionSource = remember { MutableInteractionSource() }
    val isTooltipHovered by tooltipInteractionSource.collectIsHoveredAsState()
    val popupPositionProvider = remember {
        GuidanceTooltipPositionProvider()
    }

    LaunchedEffect(isTooltipHovered) {
        onTooltipHoverChanged(isTooltipHovered)
    }

    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnClickOutside = false
        )
    ) {
        val shape = RoundedCornerShape(8.dp)
        val scrollState = rememberScrollState()

        Surface(
            shape = shape,
            shadowElevation = 4.dp,
            modifier = Modifier
                .hoverable(tooltipInteractionSource)
                .widthIn(min = 180.dp, max = 320.dp)
                .heightIn(max = 240.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(GuidanceBackground, shape)
                    .border(1.dp, GuidanceBorder, shape)
            ) {
                // Header row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GuidanceHeaderBackground, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Guidance",
                        color = GuidanceHeadingText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (showCopied) "Copied" else "Copy guidance",
                        tint = GuidanceCopyIcon,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable {
                                copyToClipboard(guidanceLines.joinToString("\n"))
                                showCopied = true
                                copyScope.launch {
                                    delay(500)
                                    showCopied = false
                                }
                            }
                    )
                }

                HorizontalDivider(color = GuidanceBorder, thickness = 0.5.dp)

                // Scrollable content
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        guidanceLines.forEach { line ->
                            Text(
                                text = line,
                                color = GuidanceBodyText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private class GuidanceTooltipPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val anchorGapPx = 12
        val windowMarginPx = 12

        val preferredX = anchorBounds.right + anchorGapPx
        val fitsOnRight = preferredX + popupContentSize.width <= windowSize.width - windowMarginPx
        val x = if (fitsOnRight) {
            preferredX
        } else {
            (anchorBounds.left - popupContentSize.width - anchorGapPx)
                .coerceAtLeast(windowMarginPx)
        }

        val centeredY = anchorBounds.top + ((anchorBounds.height - popupContentSize.height) / 2)
        val maxY = (windowSize.height - popupContentSize.height - windowMarginPx).coerceAtLeast(windowMarginPx)
        val y = centeredY.coerceIn(windowMarginPx, maxY)

        return IntOffset(x = x, y = y)
    }
}
