package com.strangeparticle.luther.client.provider.anthropic

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

internal object AnthropicProvider : AiProvider {
    override val id = "anthropic"
    override val displayName = "Anthropic"

    override fun settingsItems(): List<SettingsItem<*>> = listOf(
        AnthropicApiKeySetting,
        AnthropicPreferredModelSetting,
    )

    override fun createClient(context: SettingsItemContext): AiProviderClient {
        val apiKey = context.get(AnthropicApiKeySetting).orEmpty()
        return AiProviderClientAnthropic(
            httpClient = context.httpClient,
            apiKeyProvider = { apiKey },
        )
    }

    override fun currentModelId(context: SettingsItemContext): String =
        context.get(AnthropicPreferredModelSetting).orEmpty()

    override fun isConfigured(context: SettingsItemContext): Boolean =
        context.get(AnthropicApiKeySetting).orEmpty().isNotBlank()

    override val settingsSectionComposable: @Composable (SettingsViewModel) -> Unit = { viewModel ->
        AnthropicSettingsSectionComposable(viewModel)
    }

    fun preferredModelIds(): List<String> = listOf("claude-sonnet-4-6", "claude-3-5-sonnet-latest")
}
