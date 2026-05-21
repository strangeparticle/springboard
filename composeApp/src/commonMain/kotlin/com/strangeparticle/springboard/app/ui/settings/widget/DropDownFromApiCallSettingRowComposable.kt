package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.items.base.DropDownFromApiCallSettingsItem
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.settings.SettingRowScaffold
import com.strangeparticle.springboard.app.ui.settings.SettingRowSmallWidgetHeight
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Renders a setting whose options come from a live service call. Handles the
 * full async lifecycle (initial load, loading spinner, error display, refresh
 * button) for any item extending [DropDownFromApiCallSettingsItem].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DropDownFromApiCallSettingRowComposable(
    item: DropDownFromApiCallSettingsItem,
    viewModel: SettingsViewModel,
) {
    SettingRowScaffold(item = item, viewModel = viewModel) {
        val scope = rememberCoroutineScope()
        var loadResult by remember(item, viewModel.settingsVersion) { mutableStateOf<Result<List<DropDownOption>>?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        fun load() {
            scope.launch {
                isLoading = true
                loadResult = item.loadOptions(viewModel.itemContext())
                isLoading = false
            }
        }
        LaunchedEffect(item, viewModel.settingsVersion) { load() }

        val selectedId = viewModel.getResolvedValue(item)
        val options = loadResult?.getOrNull().orEmpty()
        val selectedDisplayName = options.firstOrNull { it.id == selectedId }?.displayName ?: selectedId
        val error = loadResult?.exceptionOrNull()

        var expanded by remember { mutableStateOf(false) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expanded && options.isNotEmpty(),
                onExpandedChange = { if (options.isNotEmpty()) expanded = it },
                modifier = Modifier.width(220.dp).testTag(TestTags.settingsDropdown(item.id)),
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
                        text = when {
                            isLoading -> "Loading…"
                            error != null -> "Error: ${error.message}"
                            options.isEmpty() && selectedDisplayName.isBlank() -> "No options"
                            else -> selectedDisplayName.ifBlank { "(select)" }
                        },
                        fontSize = 13.sp,
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExposedDropdownMenu(
                    expanded = expanded && options.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
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
            IconButton(onClick = ::load, modifier = Modifier.size(32.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload options", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
