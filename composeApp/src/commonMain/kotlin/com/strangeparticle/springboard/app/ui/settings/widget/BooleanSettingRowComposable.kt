package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem
import com.strangeparticle.springboard.app.ui.settings.SettingRowScaffold
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

@Composable
internal fun BooleanSettingRowComposable(
    item: BooleanSettingsItem,
    viewModel: SettingsViewModel,
) {
    SettingRowScaffold(item = item, viewModel = viewModel) {
        Switch(
            checked = viewModel.getResolvedValue(item),
            onCheckedChange = { newValue -> viewModel.setUserSetting(item, newValue) },
            modifier = Modifier.graphicsLayer {
                scaleX = 0.82f
                scaleY = 0.82f
            },
        )
    }
}
