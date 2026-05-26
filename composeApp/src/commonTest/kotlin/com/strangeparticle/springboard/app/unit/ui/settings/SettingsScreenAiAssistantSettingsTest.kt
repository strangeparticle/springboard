package com.strangeparticle.springboard.app.unit.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.editio.client.provider.anthropic.AnthropicApiKeySetting
import com.strangeparticle.editio.client.provider.anthropic.AnthropicPreferredModelSetting
import com.strangeparticle.editio.client.provider.openai.OpenAiApiKeySetting
import com.strangeparticle.editio.client.provider.openai.OpenAiPreferredModelSetting
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.shared.stubHttpClientForTests
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.settings.SettingsScreen
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.assertEquals
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenAiAssistantSettingsTest {

    @Test
    fun `AI assistant settings render provider setup before timeout and debug settings`() = runComposeUiTest {
        val viewModel = createSettingsViewModelWithOpenAiProvider()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onShowActiveSettings = {},
                )
            }
        }

        val providerTop = onNodeWithText("AI Provider").getUnclippedBoundsInRoot().top
        val apiKeyTop = onNodeWithText("OpenAI API Key").getUnclippedBoundsInRoot().top
        val modelTop = onNodeWithText("OpenAI model").getUnclippedBoundsInRoot().top
        val timeoutTop = onNodeWithText("AI Provider HTTP Timeout").getUnclippedBoundsInRoot().top
        val transcriptTop = onNodeWithText("Show Full Chat Transcript (for Debug)").getUnclippedBoundsInRoot().top

        onAllNodesWithText("Show Full Chat Transcript (for Debug)").assertCountEquals(1)
        assert(providerTop < apiKeyTop) { "AI provider should render before API key" }
        assert(apiKeyTop < modelTop) { "API key should render before model" }
        assert(modelTop < timeoutTop) { "Model should render before AI provider timeout" }
        assert(timeoutTop < transcriptTop) { "AI provider timeout should render before full transcript debug setting" }
    }

    @Test
    fun `OpenAI provider rows use standard settings row spacing`() = runComposeUiTest {
        val viewModel = createSettingsViewModelWithOpenAiProvider()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onShowActiveSettings = {},
                )
            }
        }

        assertEquals(
            standardAiSettingsRowGap(),
            settingsRowGapBetween(OpenAiApiKeySetting.id, OpenAiPreferredModelSetting.id),
        )
    }

    @Test
    fun `Anthropic provider rows use standard settings row spacing`() = runComposeUiTest {
        val settingsManager = createSettingsManagerForTest()
        settingsManager.setUserSetting(AiProviderSetting, "anthropic")
        settingsManager.setUserSetting(AnthropicApiKeySetting, "test-key")
        val viewModel = SettingsViewModel(
            settingsManager = settingsManager,
            httpClient = stubHttpClientForTests(),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onShowActiveSettings = {},
                )
            }
        }

        assertEquals(
            standardAiSettingsRowGap(),
            settingsRowGapBetween(AnthropicApiKeySetting.id, AnthropicPreferredModelSetting.id),
        )
    }

    private fun createSettingsViewModelWithOpenAiProvider(): SettingsViewModel {
        val settingsManager = createSettingsManagerForTest()
        settingsManager.setUserSetting(AiProviderSetting, "openai")
        settingsManager.setUserSetting(OpenAiApiKeySetting, "test-key")
        return SettingsViewModel(
            settingsManager = settingsManager,
            httpClient = stubHttpClientForTests(),
        )
    }

    private fun SemanticsNodeInteractionsProvider.standardAiSettingsRowGap() =
        settingsRowGapBetween(HttpAiProviderTimeoutSecondsSetting.id, ShowFullChatTranscriptSetting.id)

    private fun SemanticsNodeInteractionsProvider.settingsRowGapBetween(
        upperSettingId: String,
        lowerSettingId: String,
    ) =
        onNodeWithTag(TestTags.settingsRow(lowerSettingId)).getUnclippedBoundsInRoot().top -
            onNodeWithTag(TestTags.settingsRow(upperSettingId)).getUnclippedBoundsInRoot().bottom
}
