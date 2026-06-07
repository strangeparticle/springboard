package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.settings.SettingRowScaffold
import com.strangeparticle.springboard.app.ui.settings.SettingRowSmallWidgetHeight
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DropDownSettingRowComposable(
    item: DropDownSettingsItem,
    viewModel: SettingsViewModel,
) {
    SettingRowScaffold(item = item, viewModel = viewModel) {
        var expanded by remember { mutableStateOf(false) }
        val selectedId = viewModel.getResolvedValue(item)
        val selectedDisplayName = item.options.firstOrNull { it.id == selectedId }?.displayName ?: selectedId

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .width(220.dp)
                .testTag(TestTags.settingsDropdown(item.id)),
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .height(SettingRowSmallWidgetHeight)
                    .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
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
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                item.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName, fontSize = 13.sp) },
                        onClick = {
                            viewModel.setUserSetting(item, option.id)
                            expanded = false
                        },
                        modifier = Modifier.testTag(TestTags.settingsDropdownOption(item.id, option.id)),
                    )
                }
            }
        }
    }
}
