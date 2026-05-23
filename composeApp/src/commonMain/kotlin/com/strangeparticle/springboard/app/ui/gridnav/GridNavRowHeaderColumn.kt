package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.domain.model.Environment
import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

@Composable
fun GridNavRowHeaderColumn(
    sections: List<GridNavSection>,
    environments: List<Environment>,
    selectedEnvironmentId: String?,
    resourceLabelWidth: Dp,
    gridHeaderHeight: Dp,
    hoveredHeaderResourceId: String?,
    hoveredResourceId: String?,
    onHeaderResourceHover: (String?) -> Unit,
    onResourceClick: (environmentId: String, resourceId: String) -> Unit,
    onEnvironmentHeadingSelect: (String) -> Unit,
) {
    // Header/data boundary divider is rendered by GridNavHeaderResizeBoundary in GridNav.
    Column(modifier = Modifier.width(resourceLabelWidth)) {
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(CommonUiConstants.GridRowHeight))
            }

            val headingHeight = if (section.isPrimaryHeading) gridHeaderHeight else CommonUiConstants.GridRowHeight
            SectionHeading(
                section = section,
                environments = environments,
                selectedEnvironmentId = selectedEnvironmentId,
                headingHeight = headingHeight,
                onEnvironmentHeadingSelect = onEnvironmentHeadingSelect,
            )

            section.resources.forEach { resource ->
                ResourceLabelRow(
                    resource = resource,
                    section = section,
                    hoveredHeaderResourceId = hoveredHeaderResourceId,
                    hoveredResourceId = hoveredResourceId,
                    onHeaderResourceHover = onHeaderResourceHover,
                    onResourceClick = onResourceClick,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(
    section: GridNavSection,
    environments: List<Environment>,
    selectedEnvironmentId: String?,
    headingHeight: Dp,
    onEnvironmentHeadingSelect: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headingHeight),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (section.environmentId == ALL_ENVS_ENVIRONMENT_ID) {
            Text(
                text = section.headingText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .testTag(TestTags.gridSectionHeading(section.sectionId)),
            )
        } else {
            EnvironmentHeadingDropdown(
                environments = environments,
                selectedEnvironmentId = selectedEnvironmentId,
                sectionId = section.sectionId,
                onEnvironmentHeadingSelect = onEnvironmentHeadingSelect,
            )
        }
    }
}

@Composable
private fun EnvironmentHeadingDropdown(
    environments: List<Environment>,
    selectedEnvironmentId: String?,
    sectionId: String,
    onEnvironmentHeadingSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = environments.firstOrNull { it.id == selectedEnvironmentId }?.name
        ?: "Select environment..."

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .wrapContentWidth(align = Alignment.Start, unbounded = true),
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .testTag(TestTags.gridSectionHeading(sectionId)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            environments.forEach { environment ->
                DropdownMenuItem(
                    text = { Text(environment.name) },
                    onClick = {
                        onEnvironmentHeadingSelect(environment.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResourceLabelRow(
    resource: Resource,
    section: GridNavSection,
    hoveredHeaderResourceId: String?,
    hoveredResourceId: String?,
    onHeaderResourceHover: (String?) -> Unit,
    onResourceClick: (environmentId: String, resourceId: String) -> Unit,
) {
    val isRowHighlighted = hoveredHeaderResourceId == resource.id || hoveredResourceId == resource.id
    val rowBackground = if (isRowHighlighted)
        MaterialTheme.colorScheme.surfaceContainer
    else
        Color.Transparent

    val rowLabelTag = if (section.environmentId == ALL_ENVS_ENVIRONMENT_ID)
        TestTags.gridAllEnvsRowLabel(resource.id)
    else
        TestTags.gridRowLabel(resource.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CommonUiConstants.GridRowHeight)
            .background(rowBackground)
            .focusProperties { canFocus = false }
            .testTag(rowLabelTag)
            .clickable { onResourceClick(section.environmentId, resource.id) }
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
        contentAlignment = Alignment.CenterStart,
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
                overflow = TextOverflow.Ellipsis,
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
