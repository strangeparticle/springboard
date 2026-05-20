package com.strangeparticle.springboard.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.MAX_OPEN_TABS
import com.strangeparticle.springboard.app.viewmodel.TabState

data class TabBarColors(
    val container: Color,
    val activeTab: Color,
    val inactiveTab: Color,
)

object TabBarDefaults {
    @Composable
    fun colors(): TabBarColors = TabBarColors(
        container = MaterialTheme.colorScheme.surface,
        activeTab = MaterialTheme.colorScheme.surface,
        inactiveTab = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

/**
 * Tabs and the new-tab button. The AI-assistant toggle and settings gear live in
 * [com.strangeparticle.springboard.app.ui.AppBottomBar] (rendered below the chat pane).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBar(
    tabs: List<TabState>,
    activeTabId: String,
    canCreateNewTab: Boolean,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val colors = TabBarDefaults.colors()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CommonUiConstants.TabBarHeight)
            .background(colors.container)
            .padding(horizontal = 4.dp)
            .testTag(TestTags.TAB_BAR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.forEach { tab ->
                TabIndicator(
                    label = tab.label,
                    isActive = tab.tabId == activeTabId,
                    statusIcons = tabStatusIconsFor(tab),
                    tabId = tab.tabId,
                    onSelect = { onSelect(tab.tabId) },
                    onClose = { onClose(tab.tabId) },
                    modifier = Modifier
                        .widthIn(min = TabIndicatorDefaults.MinWidth, max = TabIndicatorDefaults.MaxWidth)
                        .testTag(TestTags.tabIndicator(tab.tabId)),
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            val newTabTooltipText = if (canCreateNewTab) {
                "New tab"
            } else {
                "Maximum of $MAX_OPEN_TABS tabs reached"
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(newTabTooltipText) } },
                state = rememberTooltipState(),
            ) {
                IconButton(
                    onClick = onCreate,
                    enabled = canCreateNewTab,
                    modifier = Modifier.size(24.dp).testTag(TestTags.TAB_NEW_BUTTON),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New tab",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
