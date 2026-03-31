package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.Job

@Composable
fun GridNav(
    viewModel: SpringboardViewModel,
    isShiftHeld: Boolean,
    onShiftRelease: () -> Unit
) {
    val currentSpringboard = viewModel.springboard ?: return
    val environmentId = viewModel.selectedEnvironmentId ?: return

    var hoveredAppId by remember { mutableStateOf<String?>(null) }
    var hoveredResourceId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderAppId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderResourceId by remember { mutableStateOf<String?>(null) }

    // Single active guidance tooltip — only one can be visible at a time.
    var activeGuidanceCoordinate by remember { mutableStateOf<Coordinate?>(null) }
    var guidanceDismissJob by remember { mutableStateOf<Job?>(null) }

    // When keyNav forms a full coordinate, show guidance tooltip and activator preview.
    val keyNavCoordinate = viewModel.keyNavCoordinate
    LaunchedEffect(keyNavCoordinate) {
        if (keyNavCoordinate != null) {
            val activator = currentSpringboard.indexes.activatorByCoordinate[keyNavCoordinate]
            viewModel.hoveredActivatorPreview = activatorPreviewText(activator)
            if (currentSpringboard.indexes.guidanceByCoordinate.containsKey(keyNavCoordinate)) {
                guidanceDismissJob?.cancel()
                guidanceDismissJob = null
                activeGuidanceCoordinate = keyNavCoordinate
            }
        } else {
            viewModel.hoveredActivatorPreview = null
            if (activeGuidanceCoordinate != null) {
                activeGuidanceCoordinate = null
            }
        }
    }

    val verticalScroll = rememberScrollState()

    val environmentName = currentSpringboard.environments.find { it.id == environmentId }?.name ?: environmentId

    val headerTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()
    val longestTruncatedName = remember(currentSpringboard.apps) {
        currentSpringboard.apps
            .maxByOrNull { it.name.length }
            ?.name
            ?.let(::truncateHeaderText)
            ?: ""
    }
    val measuredLongestHeader = remember(longestTruncatedName) {
        textMeasurer.measure(longestTruncatedName, headerTextStyle)
    }
    val sin45 = 0.7071f
    val gridHeaderHeight = with(LocalDensity.current) {
        val rotatedHeightPx = (measuredLongestHeader.size.width + measuredLongestHeader.size.height) * sin45
        rotatedHeightPx.toDp() + 12.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .focusProperties { canFocus = false }
    ) {
        Row(modifier = Modifier.verticalScroll(verticalScroll)) {
            GridNavResourceLabelColumn(
                environmentName = environmentName,
                resources = currentSpringboard.resources,
                gridHeaderHeight = gridHeaderHeight,
                hoveredHeaderResourceId = hoveredHeaderResourceId,
                hoveredResourceId = hoveredResourceId,
                onHeaderResourceHover = { hoveredHeaderResourceId = it },
                onResourceClick = { viewModel.activateRow(it) },
            )

            currentSpringboard.apps.forEach { app ->
                GridNavAppColumn(
                    app = app,
                    environmentId = environmentId,
                    resources = currentSpringboard.resources,
                    gridHeaderHeight = gridHeaderHeight,
                    isHeaderHighlighted = hoveredAppId == app.id || hoveredHeaderAppId == app.id,
                    isHeaderHovered = hoveredHeaderAppId == app.id,
                    viewModel = viewModel,
                    isShiftHeld = isShiftHeld,
                    hoveredAppId = hoveredAppId,
                    hoveredResourceId = hoveredResourceId,
                    onCellHover = { appId, resourceId ->
                        hoveredAppId = appId
                        hoveredResourceId = resourceId
                    },
                    activeGuidanceCoordinate = activeGuidanceCoordinate,
                    onGuidanceCoordinateChange = { activeGuidanceCoordinate = it },
                    guidanceDismissJob = guidanceDismissJob,
                    onGuidanceDismissJobChange = { guidanceDismissJob = it },
                )
            }
        }

        GridNavAppColumnHeadingHoverDetectionOverlay(
            apps = currentSpringboard.apps,
            gridHeaderHeight = gridHeaderHeight,
            horizontalOffset = CommonUiConstants.ResourceLabelWidth,
            onHoveredAppChange = { hoveredHeaderAppId = it },
            onColumnClick = { viewModel.activateColumn(it) },
        )
    }
}
