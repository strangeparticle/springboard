package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onShowActiveSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().testTag(TestTags.SETTINGS_SCREEN)) {
        // Header bar
        SettingsHeaderBar(
            title = "Settings",
            onBack = onBack,
            onRestoreDefaults = { viewModel.clearAllUserSettings() },
        )

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
    onRestoreDefaults: () -> Unit,
) {
    val currentUiBrand = LocalUiBrand.current
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
            modifier = Modifier.testTag(TestTags.SETTINGS_BACK_BUTTON),
        ) {
            Icon(
                imageVector = currentUiBrand.vectorImages.backNavigation,
                contentDescription = "Back",
                tint = currentUiBrand.customColors.navbarText,
            )
        }
        Text(
            text = title,
            color = currentUiBrand.customColors.navbarText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = onRestoreDefaults,
            modifier = Modifier.testTag(TestTags.SETTINGS_RESTORE_DEFAULTS_BUTTON),
        ) {
            Text(
                text = "Restore Defaults",
                color = currentUiBrand.customColors.navbarText,
                fontSize = 13.sp,
            )
        }
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
    val resolvedValue = viewModel.getResolvedValue(item.key)
    val effectiveSource = viewModel.getEffectiveSource(item.key)
    val showProvenance = effectiveSource != SettingsSource.APP_DEFAULT && effectiveSource != SettingsSource.USER_SETTINGS

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.key.name,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = LocalUiBrand.current.customColors.settingsDescriptionText,
                )

                if (item.type == List::class) {
                    Spacer(modifier = Modifier.height(6.dp))
                    StartupTabsDisplay(
                        currentValue = resolvedValue as? List<*>,
                    )
                }

                if (showProvenance) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OverrideMessage(
                        sourceName = effectiveSource.displayLabel(viewModel.runtimeEnvironment).lowercase(),
                        onShowActiveSettings = onShowActiveSettings,
                    )
                }
            }

            if (item.type == StringFromDropDown::class) {
                val declaration = item.defaultValue as? StringFromDropDown
                val selectedId = resolvedValue as? String
                if (declaration != null && selectedId != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    SettingsDropdown(
                        settingKey = item.key,
                        declaration = declaration,
                        selectedId = selectedId,
                        isEnabled = true,
                        onSelect = { newId ->
                            viewModel.setUserSetting(item.key, newId)
                        },
                    )
                }
            } else if (item.type == Boolean::class) {
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = resolvedValue as? Boolean ?: false,
                    onCheckedChange = { newValue ->
                        viewModel.setUserSetting(item.key, newValue)
                    },
                    enabled = true,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.82f
                        scaleY = 0.82f
                    },
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
    val currentUiBrand = LocalUiBrand.current
    val activeSettings = "Active Settings"
    val fullText = "From $sourceName. See $activeSettings for details."
    val linkStart = fullText.indexOf(activeSettings)

    val message = buildAnnotatedString {
        append(fullText)
        addLink(
            LinkAnnotation.Clickable(
                tag = "active_settings",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = currentUiBrand.customColors.settingsLinkBase,
                        textDecoration = TextDecoration.Underline,
                    ),
                    hoveredStyle = SpanStyle(
                        color = currentUiBrand.customColors.settingsLinkHover,
                        textDecoration = TextDecoration.Underline,
                        background = currentUiBrand.customColors.settingsLinkBackgroundHover,
                    ),
                    pressedStyle = SpanStyle(
                        color = currentUiBrand.customColors.settingsLinkPressed,
                        textDecoration = TextDecoration.Underline,
                        background = currentUiBrand.customColors.settingsLinkBackgroundPressed,
                    ),
                    focusedStyle = SpanStyle(
                        color = currentUiBrand.customColors.settingsLinkBase,
                        textDecoration = TextDecoration.Underline,
                        background = currentUiBrand.customColors.settingsLinkBackgroundDefault,
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
            color = currentUiBrand.customColors.settingsLinkBase,
        ),
        modifier = Modifier.testTag(TestTags.SETTINGS_OVERRIDE_WARNING),
    )
}

@Composable
private fun StartupTabsDisplay(
    currentValue: List<*>?,
) {
    val currentUiBrand = LocalUiBrand.current
    val items = currentValue?.filterIsInstance<String>() ?: emptyList()
    Column {
        if (items.isNotEmpty()) {
            for (item in items) {
                Text(
                    text = item,
                    fontSize = 12.sp,
                    color = currentUiBrand.customColors.settingsValueText,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "No startup tabs configured",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = currentUiBrand.customColors.settingsNoValueText,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    settingKey: SettingsKey,
    declaration: StringFromDropDown,
    selectedId: String,
    isEnabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplayName = declaration.displayNameFor(selectedId) ?: selectedId
    val options = declaration.dropDownOptions

    ExposedDropdownMenuBox(
        expanded = expanded && isEnabled,
        onExpandedChange = { if (isEnabled) expanded = it },
        modifier = Modifier
            .width(220.dp)
            .testTag(TestTags.settingsDropdown(SettingsKeyNaming.jsonKey(settingKey))),
    ) {
        Row(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedDisplayName,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExposedDropdownMenu(
            expanded = expanded && isEnabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName, fontSize = 13.sp) },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag(
                        TestTags.settingsDropdownOption(SettingsKeyNaming.jsonKey(settingKey), option.id),
                    ),
                )
            }
        }
    }
}

