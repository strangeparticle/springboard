package com.strangeparticle.springboard.app.luther.provider.openai

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.provider.ProviderConfig
import com.strangeparticle.luther.client.provider.openai.OpenAiConfig
import com.strangeparticle.springboard.app.luther.provider.AiProviderSettingsAdaptor
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

internal object OpenAiProviderSettingsAdaptor : AiProviderSettingsAdaptor {
    override val providerId = "openai"
    override fun settingsItems(): List<SettingsItem<*>> =
        listOf(OpenAiApiKeySetting, OpenAiPreferredModelSetting)
    override val settingsSection: @Composable (SettingsViewModel) -> Unit = { viewModel ->
        OpenAiSettingsSectionComposable(viewModel)
    }
    override fun buildProviderConfig(settings: SettingsViewModel): ProviderConfig =
        OpenAiConfig(apiKey = settings.getResolvedValue(OpenAiApiKeySetting))
}
