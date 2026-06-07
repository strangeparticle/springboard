package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem
import com.strangeparticle.springboard.app.ui.settings.SettingRowScaffold
import com.strangeparticle.springboard.app.ui.settings.SettingRowWidgetWidth
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
internal fun StringSettingRowComposable(
    item: StringSettingsItem,
    viewModel: SettingsViewModel,
) {
    SettingRowScaffold(item = item, viewModel = viewModel) {
        var showSensitiveValue by remember { mutableStateOf(false) }
        val value = viewModel.getResolvedValue(item)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue -> viewModel.setUserSetting(item, newValue) },
                singleLine = true,
                visualTransformation = if (item.isSensitive && !showSensitiveValue) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.width(SettingRowWidgetWidth),
            )
            if (item.isSensitive) {
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { showSensitiveValue = !showSensitiveValue }) {
                    Text(if (showSensitiveValue) "Hide" else "Show", fontSize = 12.sp)
                }
            }
        }
    }
}
