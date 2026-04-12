package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.Job
import kotlin.math.roundToInt

@Composable
fun GridNav(
    viewModel: SpringboardViewModel,
    isShiftHeld: Boolean,
    onShiftRelease: () -> Unit,
    zoomSelection: GridZoomSelection = GridZoomSelection.FixedZoom(100),
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
    val horizontalScroll = rememberScrollState()

    val environmentName = currentSpringboard.environments.find { it.id == environmentId }?.name ?: environmentId

    val headerNameTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val headerIdTextStyle = TextStyle(fontSize = GridNavHeaderIdTextSizeSp.sp)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Stacked text height = name line + 2dp spacer + id line. Constant per font sizing
    // and used for both initial header sizing and live truncation math.
    val stackedTextHeightPx = remember(headerNameTextStyle, headerIdTextStyle) {
        val nameHeight = textMeasurer.measure("Mg", headerNameTextStyle).size.height
        val idHeight = textMeasurer.measure("MG", headerIdTextStyle).size.height
        val spacerPx = with(density) { 2.dp.toPx() }
        nameHeight + spacerPx + idHeight
    }

    val longestTruncatedName = remember(currentSpringboard.apps) {
        currentSpringboard.apps
            .maxByOrNull { it.name.length }
            ?.name
            ?.let(::truncateHeaderText)
            ?: ""
    }
    val measuredLongestHeader = remember(longestTruncatedName) {
        textMeasurer.measure(longestTruncatedName, headerNameTextStyle)
    }
    val computedInitialHeaderHeight = with(density) {
        val rotatedHeightPx = computeRotatedHeaderHeightPx(
            measuredLongestHeader.size.width.toFloat(), stackedTextHeightPx
        )
        rotatedHeightPx.toDp() + HeaderRotationVerticalPadding
    }
    val clampedInitialHeaderHeight = computedInitialHeaderHeight.coerceIn(
        GridNavSizingConstants.MinHeaderHeight,
        GridNavSizingConstants.MaxHeaderHeight,
    )
    var gridHeaderHeight by remember(clampedInitialHeaderHeight) {
        mutableStateOf(clampedInitialHeaderHeight)
    }

    // Re-derive each app's visible header text from the current header height. The
    // available rotated stack size grows linearly with header height; converting via
    // 1/sin45 yields the maximum text width per app at this height.
    val visibleHeaderNamesByAppId = remember(currentSpringboard.apps, gridHeaderHeight) {
        val availableRotatedHeightPx = with(density) {
            (gridHeaderHeight - HeaderRotationVerticalPadding).toPx().coerceAtLeast(0f)
        }
        val maxNameWidthPx = (availableRotatedHeightPx / Sin45) - stackedTextHeightPx
        currentSpringboard.apps.associate { app ->
            app.id to truncateHeaderTextToFitWidth(
                text = app.name,
                maxWidthPx = maxNameWidthPx,
                textMeasurer = textMeasurer,
                style = headerNameTextStyle,
            )
        }
    }

    val totalGridWidth = CommonUiConstants.ResourceLabelWidth +
        CommonUiConstants.GridColumnWidth * currentSpringboard.apps.size

    val scale = zoomSelection.let { (it as GridZoomSelection.FixedZoom).percent / 100f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusProperties { canFocus = false },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Zoom + padding + fixed-width reporting. The gridZoomScale modifier
            // divides constraints by scale and reports scaled dimensions for correct
            // scroll bounds. The inner layout modifier reports a stable width
            // (totalGridWidth) so header-height resizing only changes vertical layout,
            // not horizontal centering. Children are measured with unbounded width so
            // rotated-header overflow draws past the reported right edge.
            Box(
                modifier = Modifier
                    .gridZoomScale(scale)
                    .padding(16.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(maxWidth = Constraints.Infinity)
                        )
                        val reportedWidth = totalGridWidth.roundToPx()
                        layout(reportedWidth, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            ) {
                    Box(modifier = Modifier.verticalScroll(verticalScroll)) {
                        Row(modifier = Modifier
                            .padding(end = gridHeaderHeight)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Exit) {
                                            hoveredAppId = null
                                            hoveredResourceId = null
                                            viewModel.hoveredActivatorPreview = null
                                        }
                                    }
                                }
                            }
                        ) {
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
                                    displayName = visibleHeaderNamesByAppId[app.id] ?: app.name,
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

                        // Boundary divider sits at the header/data junction. It is positioned via
                        // offset (rather than nested between two stacked rows) so the existing
                        // column-oriented layout stays intact.
                        GridNavHeaderResizeBoundary(
                            totalGridWidth = totalGridWidth,
                            onDragDelta = { deltaPx ->
                                val deltaDp = with(density) { deltaPx.toDp() }
                                gridHeaderHeight = (gridHeaderHeight + deltaDp).coerceIn(
                                    GridNavSizingConstants.MinHeaderHeight,
                                    GridNavSizingConstants.MaxHeaderHeight,
                                )
                            },
                            modifier = Modifier.offset(
                                y = gridHeaderHeight - GridNavSizingConstants.HeaderResizeThumbHeight / 2
                            ),
                        )
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


        GridNavScrollbarOverlay(
            verticalScrollState = verticalScroll,
            horizontalScrollState = horizontalScroll,
            modifier = Modifier.matchParentSize(),
        )
    }
}
