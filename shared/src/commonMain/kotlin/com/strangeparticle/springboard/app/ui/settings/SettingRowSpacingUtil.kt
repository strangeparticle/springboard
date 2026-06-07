package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val SettingRowSpacing = 10.dp

@Composable
internal fun SettingRowSpacer() {
    Spacer(modifier = Modifier.height(SettingRowSpacing))
}
