package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

@Composable
fun GridNavResourceLabelColumn(
    environmentName: String,
    resources: List<Resource>,
    gridHeaderHeight: Dp,
    hoveredHeaderResourceId: String?,
    hoveredResourceId: String?,
    onHeaderResourceHover: (String?) -> Unit,
    onResourceClick: (String) -> Unit,
) {
    Column(modifier = Modifier.width(CommonUiConstants.ResourceLabelWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeaderHeight),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = environmentName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp).testTag(TestTags.GRID_ENVIRONMENT_TITLE)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        resources.forEach { resource ->
            val isRowHighlighted = hoveredHeaderResourceId == resource.id || hoveredResourceId == resource.id
            val rowBackground = if (isRowHighlighted)
                MaterialTheme.colorScheme.surfaceContainer
            else
                Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CommonUiConstants.GridCellSize)
                    .background(rowBackground)
                    .focusProperties { canFocus = false }
                    .testTag(TestTags.gridRowLabel(resource.id))
                    .clickable { onResourceClick(resource.id) }
                    .pointerInput(resource.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> onHeaderResourceHover(resource.id)
                                    PointerEventType.Exit -> onHeaderResourceHover(null)
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
    }
}
