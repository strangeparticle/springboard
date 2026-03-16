package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.ui.theme.NavbarBackground
import com.strangeparticle.springboard.app.ui.theme.NavbarText
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
    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(NavbarBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NavbarText,
                )
            }
            Text(
                text = "Active Settings",
                color = NavbarText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Setting",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Value",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666),
                modifier = Modifier.width(80.dp),
            )
            Text(
                text = "Source",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666),
                modifier = Modifier.width(72.dp),
            )
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        // Scrollable entries
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            for (entry in viewModel.activeSettingsEntries) {
                ActiveSettingsRow(entry)
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F0F0))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSettingsRow(entry: ActiveSettingsEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.displayName,
            fontSize = 13.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f),
        )
        if (entry.tooltipText != null) {
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
                        color = Color(0xFF4A4A4A),
                        modifier = Modifier
                            .wrapContentWidth()
                            .drawBehind {
                                val y = size.height - 1.5.dp.toPx()
                                val dashWidth = 3.dp.toPx()
                                val gapWidth = 2.dp.toPx()
                                var x = 0f
                                while (x < size.width) {
                                    val endX = minOf(x + dashWidth, size.width)
                                    drawLine(
                                        color = Color(0xFF5F5F5F),
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
                color = Color(0xFF555555),
                modifier = Modifier.width(80.dp),
            )
        }
        Text(
            text = formatSourceLabel(entry.source),
            fontSize = 13.sp,
            color = sourceColor(entry.source),
            modifier = Modifier.width(72.dp),
        )
    }
}

private fun formatSourceLabel(source: SettingsSource): String = when (source) {
    SettingsSource.APP_DEFAULT -> "Default"
    SettingsSource.USER_SETTINGS -> "User"
    SettingsSource.ENVIRONMENT_VARIABLE -> "Env var"
    SettingsSource.COMMAND_LINE -> "CLI"
}

private fun sourceColor(source: SettingsSource): Color = when (source) {
    SettingsSource.APP_DEFAULT -> Color(0xFF999999)
    SettingsSource.USER_SETTINGS -> Color(0xFF1976D2)
    SettingsSource.ENVIRONMENT_VARIABLE -> Color(0xFFE65100)
    SettingsSource.COMMAND_LINE -> Color(0xFF6A1B9A)
}
