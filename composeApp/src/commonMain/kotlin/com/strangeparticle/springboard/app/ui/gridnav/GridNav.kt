package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.ui.theme.*
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

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

    val verticalScroll = rememberScrollState()

    val environmentName = currentSpringboard.environments.find { it.id == environmentId }?.name ?: environmentId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .focusProperties { canFocus = false }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
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
                        .height(CommonUiConstants.GridHeaderHeight)
                        .background(if (isHighlighted) HeaderHoverBackground else Color.Transparent)
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
                        text = app.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = GridDividerColor, thickness = 1.dp)

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
                        .background(if (rowHighlighted) RowHighlight else Color.Transparent)
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
                        val activator = viewModel.getActivatorForCell(environmentId, app.id, resource.id)
                        val hasActivator = activator != null
                        val cellInteractionSource = remember { MutableInteractionSource() }
                        val isCellHovered by cellInteractionSource.collectIsHoveredAsState()

                        LaunchedEffect(isCellHovered) {
                            if (isCellHovered) {
                                hoveredAppId = app.id
                                hoveredResourceId = resource.id
                            } else {
                                if (hoveredAppId == app.id && hoveredResourceId == resource.id) {
                                    hoveredAppId = null
                                    hoveredResourceId = null
                                }
                            }
                        }

                        val isColumnHighlighted = hoveredAppId == app.id || hoveredHeaderAppId == app.id
                        val isInMultiSelect = coordinate in viewModel.multiSelectSet

                        val cellBackground = when {
                            isCellHovered && hasActivator -> CellHighlight
                            isInMultiSelect -> CellHighlight
                            isColumnHighlighted -> ColumnHighlight
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
                                            .background(ActiveCellIndicator)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(2.dp, ActiveCellIndicator, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = GridDividerColor, thickness = 0.5.dp)
            }
        }
    }
}
