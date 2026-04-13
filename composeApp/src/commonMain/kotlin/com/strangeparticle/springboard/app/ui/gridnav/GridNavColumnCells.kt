package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.ui.guidance.GuidanceTooltip

/**
 * Renders the data cells for a single app column. Each cell shows an activator indicator
 * (if an activator exists for that coordinate) and optional guidance tooltip.
 */
@Composable
fun GridNavColumnCells(
    appId: String,
    environmentId: String,
    resources: List<Resource>,
    activatorByCoordinate: Map<Coordinate, Activator>,
    guidanceByCoordinate: Map<Coordinate, GuidanceData>,
    multiSelectSet: Set<Coordinate>,
    isColumnHighlighted: Boolean,
    isShiftHeld: Boolean,
    hoveredResourceId: String?,
    onCellActivate: (Coordinate) -> Unit,
    onToggleMultiSelect: (Coordinate) -> Unit,
    onActivatorPreviewChange: (String?) -> Unit,
    onCellHover: (appId: String?, resourceId: String?) -> Unit,
    guidanceState: GridNavGuidanceState,
) {
    val currentUiBrand = LocalUiBrand.current
    val guidanceDismissScope = rememberCoroutineScope()

    resources.forEach { resource ->
        val coordinate = Coordinate(environmentId, appId, resource.id)
        val activator = activatorByCoordinate[coordinate]
        val hasActivator = activator != null
        val cellInteractionSource = remember { MutableInteractionSource() }
        val isCellHovered by cellInteractionSource.collectIsHoveredAsState()

        val guidanceData = guidanceByCoordinate[coordinate]
        var isTooltipHovered by remember { mutableStateOf(false) }
        val isGuidanceActive = guidanceState.activeCoordinate.value == coordinate

        LaunchedEffect(isCellHovered, isTooltipHovered) {
            if (isCellHovered) {
                onCellHover(appId, resource.id)
                onActivatorPreviewChange(activatorPreviewText(activator))
                if (guidanceData != null) {
                    guidanceState.showGuidance(coordinate)
                }
            } else {
                if (isTooltipHovered && guidanceData != null) {
                    guidanceState.cancelDismiss()
                } else if (isGuidanceActive) {
                    guidanceState.beginDismiss(guidanceDismissScope, coordinate)
                }
            }
        }

        val isRowHighlighted = hoveredResourceId == resource.id
        val isInMultiSelect = coordinate in multiSelectSet

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
                .height(CommonUiConstants.GridRowHeight)
                .background(cellBackground)
                .testTag(TestTags.gridCell(appId, resource.id))
                .hoverable(cellInteractionSource)
                .then(
                    if (hasActivator) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .clickable {
                                if (isShiftHeld) {
                                    onToggleMultiSelect(coordinate)
                                } else {
                                    onCellActivate(coordinate)
                                }
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasActivator) {
                val indicatorTag = TestTags.gridCellActivatorIndicator(appId, resource.id)
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
                        .testTag(TestTags.gridCellGuidanceIndicator(appId, resource.id))
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
