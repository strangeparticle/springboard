package com.strangeparticle.springboard.app.luther.provider.anthropic

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.provider.ProviderConfig
import com.strangeparticle.luther.client.provider.anthropic.AnthropicConfig
import com.strangeparticle.springboard.app.luther.provider.AiProviderSettingsAdaptor
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

internal object AnthropicProviderSettingsAdaptor : AiProviderSettingsAdaptor {
    override val providerId = "anthropic"
    override fun settingsItems(): List<SettingsItem<*>> =
        listOf(AnthropicApiKeySetting, AnthropicPreferredModelSetting)
    override val settingsSection: @Composable (SettingsViewModel) -> Unit = { viewModel ->
        AnthropicSettingsSectionComposable(viewModel)
    }
    override fun buildProviderConfig(settings: SettingsViewModel): ProviderConfig =
        AnthropicConfig(apiKey = settings.getResolvedValue(AnthropicApiKeySetting))
    override val preferredModelSetting = AnthropicPreferredModelSetting
}
