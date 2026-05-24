package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Renders the AI Assistant section: the provider picker, then (once a real
 * provider is selected) delegates to that provider's
 * [com.strangeparticle.editio.client.provider.AiProvider.settingsSectionComposable].
 *
 * The per-provider cascade rules (api key → model, profile → region → model,
 * etc.) live in the provider, not here.
 */
@Composable
internal fun AiSettingsSectionComposable(viewModel: SettingsViewModel) {
    SettingRowComposable(item = AiProviderSetting, viewModel = viewModel)
    val selectedId = viewModel.getResolvedValue(AiProviderSetting)
    val provider = AiProviderRegistry.byId(selectedId)
    if (provider != null) {
        Spacer(modifier = Modifier.height(10.dp))
        provider.settingsSectionComposable(viewModel)
    }
    Spacer(modifier = Modifier.height(10.dp))
    SettingRowComposable(item = HttpAiProviderTimeoutSecondsSetting, viewModel = viewModel)
    Spacer(modifier = Modifier.height(10.dp))
    SettingRowComposable(item = ShowFullChatTranscriptSetting, viewModel = viewModel)
}
