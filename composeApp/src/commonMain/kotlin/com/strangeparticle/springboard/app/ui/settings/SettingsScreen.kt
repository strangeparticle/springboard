package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    currentTabSources: List<String> = emptyList(),
) {
    Column(modifier = Modifier.fillMaxSize().testTag(TestTags.SETTINGS_SCREEN)) {
        // Header bar
        SettingsHeaderBar(
            title = "Settings",
            onBack = onBack,
            onRestoreDefaults = { viewModel.clearAllUserSettings() },
            onShowActiveSettings = onShowActiveSettings,
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
                    currentTabSources = currentTabSources,
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
    onShowActiveSettings: () -> Unit,
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
        TextButton(onClick = onShowActiveSettings) {
            Text(
                text = "Active Settings",
                color = currentUiBrand.customColors.navbarText,
                fontSize = 13.sp,
            )
        }
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
    currentTabSources: List<String>,
) {
    Text(
        text = group.name,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(10.dp))

    for (item in group.settings) {
        SettingRow(item = item, viewModel = viewModel, currentTabSources = currentTabSources)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingRow(
    item: SettingItem,
    viewModel: SettingsViewModel,
    currentTabSources: List<String>,
) {
    val resolvedValue = viewModel.getResolvedValue(item.key)
    val effectiveSource = viewModel.getEffectiveSource(item.key)
    val hasUserChoice = effectiveSource == SettingsSource.USER_SETTINGS_FROM_SESSION || effectiveSource == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE

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

                if (item.key == SettingsKey.STARTUP_TABS) {
                    Spacer(modifier = Modifier.height(6.dp))
                    StartupTabsDisplay(
                        currentValue = resolvedValue as? List<*>,
                    )
                    if (currentTabSources.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.saveCurrentTabsAsStartupTabs(currentTabSources) },
                            modifier = Modifier.testTag(TestTags.SETTINGS_USE_CURRENT_TABS_BUTTON),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "Use current tabs",
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                ProvenanceLabel(
                    effectiveSource = effectiveSource,
                    runtimeEnvironment = viewModel.runtimeEnvironment,
                )
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
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.82f
                        scaleY = 0.82f
                    },
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            ClearSettingButton(
                enabled = hasUserChoice,
                fallbackSourceLabel = viewModel.fallbackSourceLabel(item.key),
                onClear = { viewModel.clearUserSetting(item.key) },
            )
        }
    }
}

@Composable
private fun ClearSettingButton(
    enabled: Boolean,
    fallbackSourceLabel: String,
    onClear: () -> Unit,
) {
    val contentDescription = if (enabled) {
        "Clear your choice \u2014 will use $fallbackSourceLabel value"
    } else {
        "No user choice to clear"
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (enabled) Modifier.clickable(role = Role.Button) { onClear() }
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            },
        )
    }
}

@Composable
private fun ProvenanceLabel(
    effectiveSource: SettingsSource,
    runtimeEnvironment: RuntimeEnvironment,
) {
    Text(
        text = "Source: ${effectiveSource.displayLabel(runtimeEnvironment).lowercase()}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                    text = "\u2022 $item",
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

