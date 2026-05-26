package com.strangeparticle.editio.client.provider.openai

import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.ui.settings.SettingRowComposable
import com.strangeparticle.springboard.app.ui.settings.SettingRowSpacer
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Renders OpenAI's settings section: api key row, then (once a key is present)
 * the preferred-model row. Two `SettingRowComposable` calls with an early return.
 */
@Composable
internal fun OpenAiSettingsSectionComposable(viewModel: SettingsViewModel) {
    SettingRowComposable(item = OpenAiApiKeySetting, viewModel = viewModel)
    val apiKey = viewModel.getResolvedValue(OpenAiApiKeySetting).orEmpty()
    if (apiKey.isBlank()) return
    SettingRowSpacer()
    SettingRowComposable(item = OpenAiPreferredModelSetting, viewModel = viewModel)
}
