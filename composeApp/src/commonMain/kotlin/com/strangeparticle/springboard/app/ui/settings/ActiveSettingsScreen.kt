package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.viewmodel.ActiveSettingsEntry
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * A read-only diagnostic screen showing all settings known to the registry
 * with their current resolved values and sources.
 */
@Composable
fun ActiveSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val currentUiBrand = LocalUiBrand.current
    Column(modifier = Modifier.fillMaxSize().testTag(TestTags.ACTIVE_SETTINGS_SCREEN)) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(currentUiBrand.customColors.navbarBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag(TestTags.ACTIVE_SETTINGS_BACK_BUTTON),
            ) {
                Icon(
                    imageVector = currentUiBrand.vectorImages.backNavigation,
                    contentDescription = "Back",
                    tint = currentUiBrand.customColors.navbarText,
                )
            }
            Text(
                text = "Active Settings",
                color = currentUiBrand.customColors.navbarText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Setting",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Value",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
            )
            Text(
                text = "Source",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp),
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // Scrollable entries
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            for (entry in viewModel.activeSettingsEntries) {
                ActiveSettingsRow(entry)
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSettingsRow(entry: ActiveSettingsEntry) {
    val currentUiBrand = LocalUiBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.displayName,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (entry.tooltipText != null) {
            val tooltipUnderlineColor = currentUiBrand.customColors.settingsTooltipUnderline
            Box(modifier = Modifier.width(80.dp)) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(entry.tooltipText)
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = entry.resolvedValue,
                        fontSize = 13.sp,
                        color = currentUiBrand.customColors.settingsTooltipValueText,
                        modifier = Modifier
                            .testTag(TestTags.activeSettingsValue(entry.displayName))
                            .wrapContentWidth()
                            .drawBehind {
                                val y = size.height - 1.5.dp.toPx()
                                val dashWidth = 3.dp.toPx()
                                val gapWidth = 2.dp.toPx()
                                var x = 0f
                                while (x < size.width) {
                                    val endX = minOf(x + dashWidth, size.width)
                                    drawLine(
                                        color = tooltipUnderlineColor,
                                        start = Offset(x, y),
                                        end = Offset(endX, y),
                                        strokeWidth = 1.4.dp.toPx(),
                                    )
                                    x += dashWidth + gapWidth
                                }
                            },
                    )
                }
            }
        } else {
            Text(
                text = entry.resolvedValue,
                fontSize = 13.sp,
                color = currentUiBrand.customColors.settingsValueText,
                modifier = Modifier.width(80.dp).testTag(TestTags.activeSettingsValue(entry.displayName)),
            )
        }
        Text(
            text = formatSourceLabel(entry.source),
            fontSize = 13.sp,
            color = sourceColor(entry.source),
            modifier = Modifier.width(72.dp).testTag(TestTags.activeSettingsSourceLabel(entry.displayName)),
        )
    }
}

private fun formatSourceLabel(source: SettingsSource): String = when (source) {
    SettingsSource.APP_DEFAULT -> "Default"
    SettingsSource.USER_SETTINGS -> "User"
    SettingsSource.ENVIRONMENT_VARIABLE -> "Env var"
    SettingsSource.PARAMS -> "Params"
}

@Composable
private fun sourceColor(source: SettingsSource): Color {
    val currentUiBrand = LocalUiBrand.current
    return when (source) {
        SettingsSource.APP_DEFAULT -> currentUiBrand.customColors.settingsSourceAppDefault
        SettingsSource.USER_SETTINGS -> MaterialTheme.colorScheme.primary
        SettingsSource.ENVIRONMENT_VARIABLE -> currentUiBrand.customColors.settingsSourceEnvironmentVariable
        SettingsSource.PARAMS -> currentUiBrand.customColors.settingsSourceParams
    }
}
