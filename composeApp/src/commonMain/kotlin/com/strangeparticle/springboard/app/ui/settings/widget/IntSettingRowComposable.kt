package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.strangeparticle.springboard.app.settings.items.base.IntSettingsItem
import com.strangeparticle.springboard.app.ui.settings.SettingRowScaffold
import com.strangeparticle.springboard.app.ui.settings.SettingRowWidgetWidth
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
internal fun IntSettingRowComposable(
    item: IntSettingsItem,
    viewModel: SettingsViewModel,
) {
    SettingRowScaffold(item = item, viewModel = viewModel) {
        var textValue by remember(item.id) { mutableStateOf(viewModel.getResolvedValue(item).toString()) }
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                val parsed = newValue.toIntOrNull()
                if (parsed != null && parsed in item.minimumValue..item.maximumValue) {
                    viewModel.setUserSetting(item, parsed)
                }
            },
            singleLine = true,
            supportingText = {
                Text("${item.minimumValue}-${item.maximumValue} seconds")
            },
            modifier = Modifier.width(SettingRowWidgetWidth),
        )
    }
}
