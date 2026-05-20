package com.strangeparticle.springboard.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.TestTags

object TabIndicatorDefaults {
    val Height = 28.dp
    val MinWidth = 80.dp
    val MaxWidth = 180.dp
}

@Composable
fun TabIndicator(
    label: String,
    isActive: Boolean,
    statusIcons: List<TabStatusIcon>,
    tabId: String,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val background = when {
        isActive && isHovered -> MaterialTheme.colorScheme.surfaceContainerLowest
        isActive -> MaterialTheme.colorScheme.surface
        isHovered -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val foreground = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .height(TabIndicatorDefaults.Height)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(background)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (statusIcons.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            statusIcons.forEachIndexed { index, statusIcon ->
                if (index > 0) Spacer(modifier = Modifier.width(2.dp))
                val (imageVector: ImageVector, tag, contentDescription) = when (statusIcon) {
                    TabStatusIcon.Dirty -> Triple(
                        Icons.Outlined.Circle,
                        TestTags.tabDirtyIndicator(tabId),
                        "Tab has unsaved changes",
                    )
                    TabStatusIcon.NonSaveable -> Triple(
                        Icons.Default.Lock,
                        TestTags.tabLockIndicator(tabId),
                        "Tab source is read-only",
                    )
                }
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = foreground,
                    modifier = Modifier.size(10.dp).testTag(tag),
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close tab $label",
                modifier = Modifier.size(12.dp),
                tint = foreground,
            )
        }
    }
}
