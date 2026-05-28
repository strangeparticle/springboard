package com.strangeparticle.luther.client.provider.openai

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

internal object OpenAiProvider : AiProvider {
    override val id = "openai"
    override val displayName = "OpenAI"

    override fun settingsItems(): List<SettingsItem<*>> = listOf(
        OpenAiApiKeySetting,
        OpenAiPreferredModelSetting,
    )

    override fun preferredModelSetting() = OpenAiPreferredModelSetting

    override fun createClient(context: SettingsItemContext): AiProviderClient {
        val apiKey = context.get(OpenAiApiKeySetting).orEmpty()
        return AiProviderClientOpenAi(
            httpClient = context.httpClient,
            apiKeyProvider = { apiKey },
        )
    }

    override fun currentModelId(context: SettingsItemContext): String =
        context.get(OpenAiPreferredModelSetting).orEmpty()

    override fun isConfigured(context: SettingsItemContext): Boolean =
        context.get(OpenAiApiKeySetting).orEmpty().isNotBlank()

    override val settingsSectionComposable: @Composable (SettingsViewModel) -> Unit = { viewModel ->
        OpenAiSettingsSectionComposable(viewModel)
    }

    /** Preferred-first list of model ids, used by [OpenAiPreferredModelSetting.loadOptions]
     *  to surface the curated ids at the top of the dropdown when the live list is fetched. */
    fun preferredModelIds(): List<String> = listOf("gpt-5", "gpt-4.1")
}
