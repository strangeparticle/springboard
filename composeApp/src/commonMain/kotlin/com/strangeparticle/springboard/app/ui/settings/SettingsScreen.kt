package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.ui.theme.color.*
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onShowActiveSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar
        SettingsHeaderBar(title = "Settings", onBack = onBack)

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Grouped settings
            for (group in viewModel.groupedSettings) {
                SettingsGroupSection(
                    group = group,
                    viewModel = viewModel,
                    onShowActiveSettings = onShowActiveSettings,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsHeaderBar(
    title: String,
    onBack: () -> Unit,
) {
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
            text = title,
            color = NavbarText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsGroupSection(
    group: com.strangeparticle.springboard.app.viewmodel.SettingsGroup,
    viewModel: SettingsViewModel,
    onShowActiveSettings: () -> Unit,
) {
    Text(
        text = group.name,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(10.dp))

    for (item in group.settings) {
        SettingRow(item = item, viewModel = viewModel, onShowActiveSettings = onShowActiveSettings)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SettingRow(
    item: SettingItem,
    viewModel: SettingsViewModel,
    onShowActiveSettings: () -> Unit,
) {
    val isOverridden = viewModel.isOverridden(item.key)
    val resolvedValue = viewModel.getResolvedValue(item.key)
    val alpha = if (isOverridden) 0.5f else 1.0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = SettingsDescriptionText,
                )

                if (item.type == FilePath::class) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilePathControl(
                        currentValue = resolvedValue as? FilePath,
                    )
                }

                if (isOverridden) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OverrideMessage(
                        sourceName = formatSourceName(viewModel.getSource(item.key)),
                        onShowActiveSettings = onShowActiveSettings,
                    )
                }
            }

            if (item.type == Boolean::class) {
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = resolvedValue as? Boolean ?: false,
                    onCheckedChange = { newValue ->
                        if (!isOverridden) {
                            viewModel.setUserSetting(item.key, newValue)
                        }
                    },
                    enabled = !isOverridden,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.82f
                        scaleY = 0.82f
                    },
                )
            } else if (item.type == FilePath::class && !isOverridden) {
                Spacer(modifier = Modifier.width(16.dp))
                FilePathActions(
                    viewModel = viewModel,
                    currentValue = resolvedValue as? FilePath,
                )
            }
        }
    }
}

@Composable
private fun OverrideMessage(
    sourceName: String,
    onShowActiveSettings: () -> Unit,
) {
    val activeSettings = "Active Settings"
    val fullText = "Overridden by $sourceName. See $activeSettings for details."
    val linkStart = fullText.indexOf(activeSettings)

    val message = buildAnnotatedString {
        append(fullText)
        addLink(
            LinkAnnotation.Clickable(
                tag = "active_settings",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = SettingsLinkBase,
                        textDecoration = TextDecoration.Underline,
                    ),
                    hoveredStyle = SpanStyle(
                        color = SettingsLinkHover,
                        textDecoration = TextDecoration.Underline,
                        background = SettingsLinkBackgroundHover,
                    ),
                    pressedStyle = SpanStyle(
                        color = SettingsLinkPressed,
                        textDecoration = TextDecoration.Underline,
                        background = SettingsLinkBackgroundPressed,
                    ),
                    focusedStyle = SpanStyle(
                        color = SettingsLinkBase,
                        textDecoration = TextDecoration.Underline,
                        background = SettingsLinkBackgroundDefault,
                    ),
                ),
                linkInteractionListener = { onShowActiveSettings() },
            ),
            start = linkStart,
            end = linkStart + activeSettings.length,
        )
    }

    Text(
        text = message,
        style = LocalTextStyle.current.copy(
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            color = SettingsLinkBase,
        ),
    )
}

@Composable
private fun FilePathControl(
    currentValue: FilePath?,
) {
    Column {
        if (currentValue != null) {
            Text(
                text = currentValue.path,
                fontSize = 12.sp,
                color = SettingsValueText,
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "No startup springboard configured",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = SettingsNoValueText,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

    }
}

@Composable
private fun FilePathActions(
    viewModel: SettingsViewModel,
    currentValue: FilePath?,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = {
                viewModel.designateCurrentFileAsStartup()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text("Use Current File", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
        }

        if (currentValue != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.clearStartupSpringboard() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text("Clear", fontSize = 12.sp)
            }
        }
    }
}

private fun formatSourceName(source: SettingsSource): String = when (source) {
    SettingsSource.APP_DEFAULT -> "app default"
    SettingsSource.USER_SETTINGS -> "user settings"
    SettingsSource.ENVIRONMENT_VARIABLE -> "environment variable"
    SettingsSource.COMMAND_LINE -> "command-line parameter"
}
