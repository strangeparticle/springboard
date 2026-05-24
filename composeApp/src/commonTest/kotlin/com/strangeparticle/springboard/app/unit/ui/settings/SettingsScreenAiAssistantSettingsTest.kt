package com.strangeparticle.springboard.app.unit.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.editio.client.provider.openai.OpenAiApiKeySetting
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.shared.stubHttpClientForTests
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.settings.SettingsScreen
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenAiAssistantSettingsTest {

    @Test
    fun `AI assistant settings render provider setup before timeout and debug settings`() = runComposeUiTest {
        val settingsManager = createSettingsManagerForTest()
        settingsManager.setUserSetting(AiProviderSetting, "openai")
        settingsManager.setUserSetting(OpenAiApiKeySetting, "test-key")
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
}
