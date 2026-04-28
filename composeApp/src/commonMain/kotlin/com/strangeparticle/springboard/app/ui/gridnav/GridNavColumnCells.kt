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
 * Renders the data cells for a single app column across all grid sections.
 * For each section, a heading-height spacer is rendered (matching the row-header
 * column's heading slot) followed by one cell per section resource. A blank
 * spacer row separates consecutive sections.
 */
@Composable
fun GridNavColumnCells(
    appId: String,
    sections: List<GridNavSection>,
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
    sections.forEachIndexed { index, section ->
        // The rotated app-column header (GridNavColumnHeader) above this composable
        // occupies the gridHeaderHeight slot for the first section, so the first
        // section's heading spacer is omitted here. Subsequent sections render a
        // GridRowHeight spacer between sections plus a GridRowHeight heading-row spacer
        // aligned with the env-name heading text in the row-header column.
        if (index > 0) {
            Box(modifier = Modifier.fillMaxWidth().height(CommonUiConstants.GridRowHeight))
            Box(modifier = Modifier.fillMaxWidth().height(CommonUiConstants.GridRowHeight))
        }

        section.resources.forEach { resource ->
            SectionCell(
                appId = appId,
                resource = resource,
                section = section,
                activatorByCoordinate = activatorByCoordinate,
                guidanceByCoordinate = guidanceByCoordinate,
                multiSelectSet = multiSelectSet,
                isColumnHighlighted = isColumnHighlighted,
                isShiftHeld = isShiftHeld,
                hoveredResourceId = hoveredResourceId,
                onCellActivate = onCellActivate,
                onToggleMultiSelect = onToggleMultiSelect,
                onActivatorPreviewChange = onActivatorPreviewChange,
                onCellHover = onCellHover,
                guidanceState = guidanceState,
            )
        }
    }
}

@Composable
private fun SectionCell(
    appId: String,
    resource: Resource,
    section: GridNavSection,
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

    val coordinate = Coordinate(section.environmentId, appId, resource.id)
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

    val isAllEnvsSection = section.environmentId == ALL_ENVS_ENVIRONMENT_ID
    val cellTag = if (isAllEnvsSection)
        TestTags.gridAllEnvsCell(appId, resource.id)
    else
        TestTags.gridCell(appId, resource.id)
    val activatorIndicatorTag = if (isAllEnvsSection)
        TestTags.gridAllEnvsCellActivatorIndicator(appId, resource.id)
    else
        TestTags.gridCellActivatorIndicator(appId, resource.id)
    val guidanceIndicatorTag = if (isAllEnvsSection)
        TestTags.gridAllEnvsCellGuidanceIndicator(appId, resource.id)
    else
        TestTags.gridCellGuidanceIndicator(appId, resource.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CommonUiConstants.GridRowHeight)
            .background(cellBackground)
            .testTag(cellTag)
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
        contentAlignment = Alignment.Center,
    ) {
        if (hasActivator) {
            if (isCellHovered) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .testTag(activatorIndicatorTag)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .testTag(activatorIndicatorTag)
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
                    .testTag(guidanceIndicatorTag),
            )
        }

        if (isGuidanceActive && guidanceData != null) {
            GuidanceTooltip(
                guidanceLines = guidanceData.guidanceLines,
                onTooltipHoverChanged = { hovered -> isTooltipHovered = hovered },
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
}
