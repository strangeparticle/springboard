package com.strangeparticle.editio.client.provider.anthropic

import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.ui.settings.SettingRowComposable
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Renders Anthropic's settings section: api key row, then (once a key is present)
 * the preferred-model row.
 */
@Composable
internal fun AnthropicSettingsSectionComposable(viewModel: SettingsViewModel) {
    SettingRowComposable(item = AnthropicApiKeySetting, viewModel = viewModel)
    val apiKey = viewModel.getResolvedValue(AnthropicApiKeySetting).orEmpty()
    if (apiKey.isBlank()) return
    SettingRowComposable(item = AnthropicPreferredModelSetting, viewModel = viewModel)
}
