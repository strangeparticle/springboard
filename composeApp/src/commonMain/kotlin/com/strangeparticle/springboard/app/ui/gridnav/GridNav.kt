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
import androidx.compose.ui.platform.testTag
import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.domain.model.AppColumn
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.SeparatorColumn
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.appColumnLayout
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import kotlin.math.roundToInt

@Composable
fun GridNav(
    springboard: Springboard,
    selectedEnvironmentId: String?,
    multiSelectSet: Set<Coordinate>,
    keyNavCoordinate: Coordinate?,
    isShiftHeld: Boolean,
    onCellActivate: (Coordinate) -> Unit,
    onColumnActivate: (environmentId: String, appId: String) -> Unit,
    onRowActivate: (environmentId: String, resourceId: String) -> Unit,
    onToggleMultiSelect: (Coordinate) -> Unit,
    onActivatorPreviewChange: (String?) -> Unit,
    zoomSelection: GridZoomSelection = GridZoomSelection.FixedZoom(100),
) {

    // Hover state is tracked at two levels: cell-level (hoveredAppId/hoveredResourceId)
    // for crosshair-style column+row highlighting as the pointer moves over data cells,
    // and header-level (hoveredHeaderAppId/hoveredHeaderResourceId) for highlighting
    // when the pointer is over a column or row header label rather than a data cell.
    var hoveredAppId by remember { mutableStateOf<String?>(null) }
    var hoveredResourceId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderAppId by remember { mutableStateOf<String?>(null) }
    var hoveredHeaderResourceId by remember { mutableStateOf<String?>(null) }

    // Manages the lifecycle of the single visible guidance tooltip in the grid.
    // Shared across all cells so that showing guidance on one cell automatically
    // dismisses any tooltip on a previously hovered cell.
    val guidanceState = rememberGridNavGuidanceState()

    // Keyboard navigation can form a full coordinate (environment + app + resource)
    // without any pointer interaction. When it does, show the same guidance tooltip
    // and activator preview that pointer hover would show. When keyNav clears, clear both.
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

    val sections = remember(springboard, selectedEnvironmentId) {
        buildGridNavSections(springboard, selectedEnvironmentId)
    }

    // Header sizing is derived from font metrics and the longest app name. The
    // initial height is clamped to min/max bounds. gridHeaderHeight is mutable
    // because the user can drag the header resize boundary to adjust it.
    val headerSizing = rememberGridNavHeaderSizing(springboard.apps)
    val density = LocalDensity.current

    var gridHeaderHeight by remember(headerSizing.initialHeaderHeight) {
        mutableStateOf(headerSizing.initialHeaderHeight)
    }

    // Re-derive each app's visible header text whenever the header height changes.
    // Shorter headers truncate long app names; taller headers reveal more text.
    val visibleHeaderNamesByAppId = remember(springboard.apps, gridHeaderHeight) {
        computeVisibleHeaderNames(
            springboard.apps,
            gridHeaderHeight,
            headerSizing.stackedTextHeightPx,
            headerSizing.textMeasurer,
            headerSizing.density,
        )
    }

    // Visual order of columns: apps grouped by `appGroupId` with blank separator
    // slots between adjacent groups. Falls back to one AppColumn per app in
    // declaration order when no `appGroups` are declared.
    val columnLayout = remember(springboard) { springboard.appColumnLayout() }

    // The logical grid width used for centering and scroll bounds. Does not
    // include the rotated header overflow — that extends past the right edge
    // visually but is not part of the scrollable content width. Separator slots
    // count toward the width since they each occupy one full GridColumnWidth.
    val totalGridWidth = CommonUiConstants.ResourceLabelWidth +
        CommonUiConstants.GridColumnWidth * columnLayout.size

    val scale = zoomSelection.let { (it as GridZoomSelection.FixedZoom).percent / 100f }

    // Outermost container: fills the available space, paints the surface
    // background, disables focus (keyboard nav is handled by NavBar above),
    // and provides horizontal scrolling. TopCenter alignment centers the
    // grid horizontally when it's narrower than the viewport.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusProperties { canFocus = false }
            .horizontalScroll(horizontalScroll),
        contentAlignment = Alignment.TopCenter,
    ) {
        // This Box applies three modifiers that must be layered in this order:
        //
        // 1. gridZoomScale — scales children visually and divides incoming
        //    constraints by the scale factor so content lays out at 1:1 in its
        //    own coordinate space. Reports scaled dimensions back to the parent
        //    so horizontal scroll bounds stay correct at any zoom level.
        //
        // 2. padding — 16dp gutter around the grid, applied after zoom so the
        //    padding itself scales with the grid.
        //
        // 3. Fixed-width layout — measures children with unbounded width (so
        //    rotated headers can overflow past the grid's right edge without
        //    being clipped), then reports totalGridWidth as the measured width.
        //    This prevents header-height resizing from changing the reported
        //    width, which would cause the parent's centering to shift
        //    horizontally while dragging the header resize handle.
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
            // Vertical scroll container for the grid content.
            Box(modifier = Modifier.verticalScroll(verticalScroll)) {

                // The Row holds the row-header column followed by each app column.
                // padding(end) reserves space for rotated headers that extend past
                // the last column's right edge. The pointerInput clears all hover
                // state (column highlight, row highlight, activator preview) when
                // the pointer leaves the grid area entirely — individual cells
                // handle their own hover-enter but only this top-level handler
                // can detect that the pointer has left the grid as a whole.
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
                        sections = sections,
                        gridHeaderHeight = gridHeaderHeight,
                        hoveredHeaderResourceId = hoveredHeaderResourceId,
                        hoveredResourceId = hoveredResourceId,
                        onHeaderResourceHover = { hoveredHeaderResourceId = it },
                        onResourceClick = onRowActivate,
                    )

                    // Each layout slot gets a fixed-width column. AppColumn slots
                    // render the rotated header and data cells; SeparatorColumn slots
                    // render an empty fixed-width Box that visually separates app
                    // groups. A column is highlighted when either a data cell in
                    // that column is hovered (hoveredAppId) or the column header
                    // itself is hovered via the overlay (hoveredHeaderAppId).
                    columnLayout.forEachIndexed { slotIndex, slot ->
                        when (slot) {
                            is AppColumn -> {
                                val app = slot.app
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
                                        sections = sections,
                                        activatorByCoordinate = springboard.indexes.activatorByCoordinate,
                                        guidanceByCoordinate = springboard.indexes.guidanceByCoordinate,
                                        multiSelectSet = multiSelectSet,
                                        isColumnHighlighted = isHeaderHighlighted,
                                        isShiftHeld = isShiftHeld,
                                        hoveredResourceId = hoveredResourceId,
                                        hoveredHeaderResourceId = hoveredHeaderResourceId,
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
                            SeparatorColumn -> {
                                Box(
                                    modifier = Modifier
                                        .width(CommonUiConstants.GridColumnWidth)
                                        .testTag(TestTags.gridColumnSeparator(slotIndex))
                                )
                            }
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

            // Transparent overlay that sits on top of the header area and provides
            // parallelogram-shaped hover detection for the rotated column headers.
            // Compose hit-testing is always rectangular, so without this overlay
            // the diagonal header shapes would have incorrect hover regions. This
            // overlay uses a coordinate transform (effectiveX = pointerX + pointerY
            // - headerHeight) to map pointer positions into the correct column.
            // It must be a sibling of the vertical-scroll Box (not inside it) so
            // it stays fixed at the top while data cells scroll underneath.
            GridNavAppColumnHeadingHoverDetectionOverlay(
                columnLayout = columnLayout,
                gridHeaderHeight = gridHeaderHeight,
                horizontalOffset = CommonUiConstants.ResourceLabelWidth,
                onHoveredAppChange = { hoveredHeaderAppId = it },
                onColumnClick = { appId -> onColumnActivate(selectedEnvironmentId ?: ALL_ENVS_ENVIRONMENT_ID, appId) },
            )
        }
    }
}
