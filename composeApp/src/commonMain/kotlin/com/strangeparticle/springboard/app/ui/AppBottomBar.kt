package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand

/**
 * Persistent bottom-of-window bar that hosts global app controls (AI assistant toggle and
 * settings gear, currently). Always visible below the chat pane (when shown) and below the
 * tab bar. Mirrors the [com.strangeparticle.springboard.app.ui.tabs.TabBar] styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBottomBar(
    isAssistantConfigured: Boolean,
    isAssistantOpen: Boolean,
    onToggleAssistant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUiBrand = LocalUiBrand.current
    val assistantIconTint = if (isAssistantOpen) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = modifier.fillMaxWidth()) {
        // Thin divider separates this bar from the tab row (or chat pane) above it.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CommonUiConstants.BottomBarHeight)
                // One step higher tone than the tab row's surfaceContainerLow so the
                // bottom bar reads as a distinct strip even with the divider.
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(if (isAssistantConfigured) "AI assistant" else "AI assistant (configure in settings)")
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(
                    onClick = onToggleAssistant,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(TestTags.ASSISTANT_TOGGLE_BUTTON),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = if (isAssistantConfigured) "AI assistant" else "AI assistant (not configured)",
                        modifier = Modifier.size(14.dp),
                        tint = assistantIconTint,
                    )
                }
            }

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Settings") } },
                state = rememberTooltipState(),
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(24.dp).testTag(TestTags.SETTINGS_GEAR_ICON),
                ) {
                    Icon(
                        imageVector = currentUiBrand.vectorImages.settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
