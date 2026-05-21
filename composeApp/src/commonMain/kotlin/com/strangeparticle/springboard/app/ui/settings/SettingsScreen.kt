package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import com.strangeparticle.springboard.app.settings.items.base.ListOfStringSettingsItem
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.ui.settings.widget.ListOfStringSettingRowComposable
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onShowActiveSettings: () -> Unit,
    currentTabSources: List<String> = emptyList(),
    showAiSettingsFirst: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxSize().testTag(TestTags.SETTINGS_SCREEN)) {
        SettingsHeaderBar(
            title = "Settings",
            onBack = onBack,
            onRestoreDefaults = { viewModel.clearAllUserSettings() },
            onShowActiveSettings = onShowActiveSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (showAiSettingsFirst) {
                renderAiAssistantGroup(viewModel)
                Spacer(modifier = Modifier.height(20.dp))
            }

            for (groupedItems in viewModel.groupedSettings) {
                if (groupedItems.group == SettingsGroup.AiAssistant) {
                    if (!showAiSettingsFirst) {
                        renderAiAssistantGroup(viewModel)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    continue
                }

                SettingsGroupSectionComposable(
                    groupName = groupedItems.group.displayName(),
                    items = groupedItems.items,
                    viewModel = viewModel,
                    currentTabSources = currentTabSources,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun renderAiAssistantGroup(viewModel: SettingsViewModel) {
    Text(
        text = SettingsGroup.AiAssistant.displayName(),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(10.dp))
    AiSettingsSectionComposable(viewModel)
}

private fun SettingsGroup.displayName(): String = when (this) {
    SettingsGroup.General -> "General"
    SettingsGroup.DesktopMacOS -> "Desktop macOS"
    SettingsGroup.DeveloperTools -> "Developer Tools"
    SettingsGroup.AiAssistant -> "AI Assistant"
}

@Composable
private fun SettingsGroupSectionComposable(
    groupName: String,
    items: List<com.strangeparticle.springboard.app.settings.SettingsItem<*>>,
    viewModel: SettingsViewModel,
    currentTabSources: List<String>,
) {
    Text(
        text = groupName,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(10.dp))

    for (item in items) {
        // StartupTabs gets a special "Use current tabs" affordance below its
        // value list. Other list-of-string settings render via the generic
        // widget — they don't have a corresponding action.
        if (item === StartupTabsSetting) {
            ListOfStringSettingRowComposable(
                item = item as ListOfStringSettingsItem,
                viewModel = viewModel,
                extraContent = {
                    if (currentTabSources.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.saveCurrentTabsAsStartupTabs(currentTabSources) },
                            modifier = Modifier.testTag(TestTags.SETTINGS_USE_CURRENT_TABS_BUTTON),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(text = "Use current tabs", fontSize = 12.sp)
                        }
                    }
                },
            )
        } else {
            SettingRowComposable(item = item, viewModel = viewModel)
        }
        Spacer(modifier = Modifier.height(10.dp))
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
