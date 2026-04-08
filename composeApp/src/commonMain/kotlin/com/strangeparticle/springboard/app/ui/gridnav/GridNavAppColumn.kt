package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.ui.guidance.GuidanceTooltip
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GridNavAppColumn(
    app: App,
    displayName: String,
    environmentId: String,
    resources: List<Resource>,
    gridHeaderHeight: Dp,
    isHeaderHighlighted: Boolean,
    isHeaderHovered: Boolean,
    viewModel: SpringboardViewModel,
    isShiftHeld: Boolean,
    hoveredAppId: String?,
    hoveredResourceId: String?,
    onCellHover: (appId: String?, resourceId: String?) -> Unit,
    activeGuidanceCoordinate: Coordinate?,
    onGuidanceCoordinateChange: (Coordinate?) -> Unit,
    guidanceDismissJob: Job?,
    onGuidanceDismissJobChange: (Job?) -> Unit,
) {
    val currentSpringboard = viewModel.springboard ?: return
    val currentUiBrand = LocalUiBrand.current
    val guidanceDismissScope = rememberCoroutineScope()

    val headerHighlightColor = if (isHeaderHovered)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceContainer

    Column(modifier = Modifier.width(CommonUiConstants.GridCellSize)) {
        // Header cell with parallelogram highlight
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
                        val sin45 = 0.7071f
                        val rotatedBottomY = (placeable.width + placeable.height) * sin45 / 2f
                        val uniformShift = placeable.height * sin45 / 4f
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
                        style = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Shift the id right by (spacer + id line height) pre-rotation so
                    // that after the -45° rotation its left-bottom corner lands on the
                    // same horizontal line as the name's left-bottom corner.
                    Text(
                        text = app.id.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = TextStyle(
                            fontSize = GridNavHeaderIdTextSizeSp.sp,
                            lineHeight = GridNavHeaderIdTextSizeSp.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(x = 14.dp),  // shift is here
                    )
                }
            }
        }

        // Header/data boundary divider is rendered by GridNavHeaderResizeBoundary in GridNav.

        // Data cells
        resources.forEach { resource ->
            val coordinate = Coordinate(environmentId, app.id, resource.id)
            val activator = viewModel.getActivatorForCell(coordinate)
            val hasActivator = activator != null
            val cellInteractionSource = remember { MutableInteractionSource() }
            val isCellHovered by cellInteractionSource.collectIsHoveredAsState()

            val guidanceData = currentSpringboard.indexes.guidanceByCoordinate[coordinate]
            var isTooltipHovered by remember { mutableStateOf(false) }
            val isGuidanceActive = activeGuidanceCoordinate == coordinate

            LaunchedEffect(isCellHovered, isTooltipHovered) {
                if (isCellHovered) {
                    onCellHover(app.id, resource.id)
                    viewModel.hoveredActivatorPreview = activatorPreviewText(activator)
                    if (guidanceData != null) {
                        onGuidanceDismissJobChange(null)
                        guidanceDismissJob?.cancel()
                        onGuidanceCoordinateChange(coordinate)
                    }
                } else {
                    if (isTooltipHovered && guidanceData != null) {
                        onGuidanceDismissJobChange(null)
                        guidanceDismissJob?.cancel()
                    } else if (isGuidanceActive) {
                        guidanceDismissJob?.cancel()
                        onGuidanceDismissJobChange(
                            guidanceDismissScope.launch {
                                delay(300L)
                                if (activeGuidanceCoordinate == coordinate) {
                                    onGuidanceCoordinateChange(null)
                                }
                            }
                        )
                    }
                }
            }

            val isColumnHighlighted = hoveredAppId == app.id || isHeaderHighlighted
            val isRowHighlighted = hoveredResourceId == resource.id
            val isInMultiSelect = coordinate in viewModel.multiSelectSet

            val cellBackground = when {
                isCellHovered && hasActivator -> MaterialTheme.colorScheme.surfaceContainer
                isInMultiSelect -> MaterialTheme.colorScheme.surfaceContainer
                isColumnHighlighted -> MaterialTheme.colorScheme.surfaceContainer
                isRowHighlighted -> MaterialTheme.colorScheme.surfaceContainer
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CommonUiConstants.GridCellSize)
                    .background(cellBackground)
                    .testTag(TestTags.gridCell(app.id, resource.id))
                    .hoverable(cellInteractionSource)
                    .then(
                        if (hasActivator) {
                            Modifier
                                .focusProperties { canFocus = false }
                                .clickable {
                                    if (isShiftHeld) {
                                        viewModel.toggleMultiSelect(coordinate)
                                    } else {
                                        viewModel.activateCell(coordinate)
                                    }
                                }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasActivator) {
                    val indicatorTag = TestTags.gridCellActivatorIndicator(app.id, resource.id)
                    if (isCellHovered) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .testTag(indicatorTag)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .testTag(indicatorTag)
                                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        )
                    }
                }

                if (hasActivator && guidanceData != null) {
                    GridNavGuidanceCornerIndicator(
                        color = currentUiBrand.customColors.guidanceIndicator,
                        creaseColor = currentUiBrand.customColors.settingsTooltipUnderline,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .testTag(TestTags.gridCellGuidanceIndicator(app.id, resource.id))
                    )
                }

                if (isGuidanceActive && guidanceData != null) {
                    GuidanceTooltip(
                        guidanceLines = guidanceData.guidanceLines,
                        onTooltipHoverChanged = { hovered ->
                            isTooltipHovered = hovered
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
    }
}
