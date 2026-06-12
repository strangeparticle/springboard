package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.luther.provider.AiProviderSettingsAdaptorRegistry
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Renders the AI Assistant section: the provider picker, then (once a real
 * provider is selected) delegates to that provider's settings-adaptor section.
 *
 * The per-provider cascade rules (api key → model, profile → region → model,
 * etc.) live in the provider's adaptor, not here.
 */
@Composable
internal fun AiSettingsSectionComposable(viewModel: SettingsViewModel) {
    SettingRowComposable(item = AiProviderSetting, viewModel = viewModel)
    val selectedId = viewModel.getResolvedValue(AiProviderSetting)
    val adaptor = AiProviderSettingsAdaptorRegistry.byId(selectedId)
    if (adaptor != null) {
        SettingRowSpacer()
        adaptor.settingsSection(viewModel)
    }
    SettingRowSpacer()
    SettingRowComposable(item = HttpAiProviderTimeoutSecondsSetting, viewModel = viewModel)
    SettingRowSpacer()
    SettingRowComposable(item = ShowFullChatTranscriptSetting, viewModel = viewModel)
}
