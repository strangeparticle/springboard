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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.ui.guidance.GuidanceTooltip
import com.strangeparticle.springboard.app.ui.brand.*
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val guidanceDismissScope = rememberCoroutineScope()
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
        rotatedHeightPx.toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .focusProperties { canFocus = false }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeaderHeight),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = environmentName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(CommonUiConstants.ResourceLabelWidth)
                    .padding(bottom = 8.dp)
            )

            currentSpringboard.apps.forEach { app ->
                val isHighlighted = hoveredAppId == app.id || hoveredHeaderAppId == app.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isHighlighted) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                        .focusProperties { canFocus = false }
                        .clickable { viewModel.activateColumn(app.id) }
                        .pointerInput(app.id) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Enter -> hoveredHeaderAppId = app.id
                                        PointerEventType.Exit -> hoveredHeaderAppId = null
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = truncateHeaderText(app.name),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(maxWidth = 10_000)
                                )
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    val sin45 = 0.7071f
                                    val cos45 = 0.7071f
                                    val rotatedBottomOffset = (placeable.width + placeable.height) * sin45 / 2f
                                    placeable.place(
                                        x = constraints.maxWidth / 2 - (placeable.width * (1f - cos45) / 2f).toInt(),
                                        y = (constraints.maxHeight - placeable.height / 2f - rotatedBottomOffset).toInt()
                                    )
                                }
                            }
                            .rotate(-45f)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Column(
            modifier = Modifier.verticalScroll(verticalScroll)
        ) {
            currentSpringboard.resources.forEach { resource ->
                val isRowHovered = hoveredResourceId == resource.id
                val isRowHeaderHovered = hoveredHeaderResourceId == resource.id
                val rowHighlighted = isRowHovered || isRowHeaderHovered

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rowHighlighted) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .width(CommonUiConstants.ResourceLabelWidth)
                            .height(CommonUiConstants.GridCellSize)
                            .focusProperties { canFocus = false }
                            .clickable { viewModel.activateRow(resource.id) }
                            .pointerInput(resource.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> hoveredHeaderResourceId = resource.id
                                            PointerEventType.Exit -> hoveredHeaderResourceId = null
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = resource.name,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    currentSpringboard.apps.forEach { app ->
                        val coordinate = Coordinate(environmentId, app.id, resource.id)
                        val activator = viewModel.getActivatorForCell(coordinate)
                        val hasActivator = activator != null
                        val cellInteractionSource = remember { MutableInteractionSource() }
                        val isCellHovered by cellInteractionSource.collectIsHoveredAsState()

                        // Guidance tooltip state
                        val guidanceData = currentSpringboard.indexes.guidanceByCoordinate[coordinate]
                        var isTooltipHovered by remember { mutableStateOf(false) }
                        val isGuidanceActive = activeGuidanceCoordinate == coordinate

                        LaunchedEffect(isCellHovered, isTooltipHovered) {
                            if (isCellHovered) {
                                hoveredAppId = app.id
                                hoveredResourceId = resource.id
                                viewModel.hoveredActivatorPreview = activatorPreviewText(activator)
                                if (guidanceData != null) {
                                    guidanceDismissJob?.cancel()
                                    guidanceDismissJob = null
                                    activeGuidanceCoordinate = coordinate
                                }
                            } else {
                                if (hoveredAppId == app.id && hoveredResourceId == resource.id) {
                                    hoveredAppId = null
                                    hoveredResourceId = null
                                    viewModel.hoveredActivatorPreview = null
                                }
                                if (isTooltipHovered && guidanceData != null) {
                                    guidanceDismissJob?.cancel()
                                    guidanceDismissJob = null
                                } else if (isGuidanceActive) {
                                    guidanceDismissJob?.cancel()
                                    guidanceDismissJob = guidanceDismissScope.launch {
                                        delay(300L)
                                        if (activeGuidanceCoordinate == coordinate) {
                                            activeGuidanceCoordinate = null
                                        }
                                    }
                                }
                            }
                        }

                        val isColumnHighlighted = hoveredAppId == app.id || hoveredHeaderAppId == app.id
                        val isInMultiSelect = coordinate in viewModel.multiSelectSet

                        val cellBackground = when {
                            isCellHovered && hasActivator -> MaterialTheme.colorScheme.surfaceContainer
                            isInMultiSelect -> MaterialTheme.colorScheme.surfaceContainer
                            isColumnHighlighted -> MaterialTheme.colorScheme.surfaceContainer
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(CommonUiConstants.GridCellSize)
                                .background(cellBackground)
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
                                if (isCellHovered) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    )
                                }
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
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        }
    }
}
