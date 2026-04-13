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
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import kotlin.math.roundToInt

@Composable
fun GridNav(
    springboard: Springboard,
    selectedEnvironmentId: String,
    multiSelectSet: Set<Coordinate>,
    keyNavCoordinate: Coordinate?,
    isShiftHeld: Boolean,
    onCellActivate: (Coordinate) -> Unit,
    onColumnActivate: (String) -> Unit,
    onRowActivate: (String) -> Unit,
    onToggleMultiSelect: (Coordinate) -> Unit,
    onActivatorPreviewChange: (String?) -> Unit,
    zoomSelection: GridZoomSelection = GridZoomSelection.FixedZoom(100),
) {

    var hoveredAppId by remember { mutableStateOf<String?>(null) }
    var hoveredResourceId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderAppId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderResourceId by remember { mutableStateOf<String?>(null) }

    val guidanceState = rememberGridNavGuidanceState()

    // When keyNav forms a full coordinate, show guidance tooltip and activator preview.
    LaunchedEffect(keyNavCoordinate) {
        if (keyNavCoordinate != null) {
            val activator = springboard.indexes.activatorByCoordinate[keyNavCoordinate]
            onActivatorPreviewChange(activatorPreviewText(activator))
            if (springboard.indexes.guidanceByCoordinate.containsKey(keyNavCoordinate)) {
                guidanceState.showGuidance(keyNavCoordinate)
            }
        } else {
            onActivatorPreviewChange(null)
            guidanceState.clearGuidance()
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val environmentName = springboard.environments.find { it.id == selectedEnvironmentId }?.name ?: selectedEnvironmentId

    val headerSizing = rememberGridNavHeaderSizing(springboard.apps)
    val density = LocalDensity.current

    var gridHeaderHeight by remember(headerSizing.initialHeaderHeight) {
        mutableStateOf(headerSizing.initialHeaderHeight)
    }

    val visibleHeaderNamesByAppId = remember(springboard.apps, gridHeaderHeight) {
        computeVisibleHeaderNames(
            springboard.apps,
            gridHeaderHeight,
            headerSizing.stackedTextHeightPx,
            headerSizing.textMeasurer,
            headerSizing.density,
        )
    }

    val totalGridWidth = CommonUiConstants.ResourceLabelWidth +
        CommonUiConstants.GridColumnWidth * springboard.apps.size

    val scale = zoomSelection.let { (it as GridZoomSelection.FixedZoom).percent / 100f }

    // Background surface and root for scrollbar overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusProperties { canFocus = false },
    ) {
        // Horizontal scroll and centering
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Zoom scaling, padding, and fixed-width reporting for stable centering
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
                    // Vertical scroll
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
                                            onActivatorPreviewChange(null)
                                        }
                                    }
                                }
                            }
                        ) {
                            GridNavRowHeaderColumn(
                                environmentName = environmentName,
                                resources = springboard.resources,
                                gridHeaderHeight = gridHeaderHeight,
                                hoveredHeaderResourceId = hoveredHeaderResourceId,
                                hoveredResourceId = hoveredResourceId,
                                onHeaderResourceHover = { hoveredHeaderResourceId = it },
                                onResourceClick = onRowActivate,
                            )

                            springboard.apps.forEach { app ->
                                val isHeaderHighlighted = hoveredAppId == app.id || hoveredHeaderAppId == app.id

                                Column(modifier = Modifier.width(CommonUiConstants.GridColumnWidth)) {
                                    GridNavColumnHeader(
                                        appId = app.id,
                                        displayName = visibleHeaderNamesByAppId[app.id] ?: app.name,
                                        gridHeaderHeight = gridHeaderHeight,
                                        isHeaderHighlighted = isHeaderHighlighted,
                                        isHeaderHovered = hoveredHeaderAppId == app.id,
                                    )

                                    // Header/data boundary divider is rendered by
                                    // GridNavHeaderResizeBoundary below.

                                    GridNavColumnCells(
                                        appId = app.id,
                                        environmentId = selectedEnvironmentId,
                                        resources = springboard.resources,
                                        activatorByCoordinate = springboard.indexes.activatorByCoordinate,
                                        guidanceByCoordinate = springboard.indexes.guidanceByCoordinate,
                                        multiSelectSet = multiSelectSet,
                                        isColumnHighlighted = isHeaderHighlighted,
                                        isShiftHeld = isShiftHeld,
                                        hoveredResourceId = hoveredResourceId,
                                        onCellActivate = onCellActivate,
                                        onToggleMultiSelect = onToggleMultiSelect,
                                        onActivatorPreviewChange = onActivatorPreviewChange,
                                        onCellHover = { appId, resourceId ->
                                            hoveredAppId = appId
                                            hoveredResourceId = resourceId
                                        },
                                        guidanceState = guidanceState,
                                    )
                                }
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
                        apps = springboard.apps,
                        gridHeaderHeight = gridHeaderHeight,
                        horizontalOffset = CommonUiConstants.ResourceLabelWidth,
                        onHoveredAppChange = { hoveredHeaderAppId = it },
                        onColumnClick = onColumnActivate,
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
