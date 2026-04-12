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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

@Composable
fun GridNavRowHeaderColumn(
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

        // Header/data boundary divider is rendered by GridNavHeaderResizeBoundary in GridNav.

        resources.forEach { resource ->
            val isRowHighlighted = hoveredHeaderResourceId == resource.id || hoveredResourceId == resource.id
            val rowBackground = if (isRowHighlighted)
                MaterialTheme.colorScheme.surfaceContainer
            else
                Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CommonUiConstants.GridRowHeight)
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
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = resource.name,
                        style = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resource.id.uppercase(),
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
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
    }
}
