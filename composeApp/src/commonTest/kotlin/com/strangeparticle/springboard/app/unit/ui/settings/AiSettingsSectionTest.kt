package com.strangeparticle.springboard.app.unit.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.settings.AiSettingsSection
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class AiSettingsSectionTest {

    @Test
    fun `renders provider dropdown and masked api key fields`() = runComposeUiTest {
        val viewModel = createViewModel()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_PROVIDER_DROPDOWN).assertExists()
        onNodeWithTag(TestTags.AI_OPENAI_API_KEY_FIELD).assertExists()
        onNodeWithTag(TestTags.AI_ANTHROPIC_API_KEY_FIELD).assertExists()
    }

    @Test
    fun `fetch models calls selected provider and stores preferred model`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        var fetchedProvider: AiProvider? = null
        var fetchedApiKey: String? = null

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(
                    viewModel = viewModel,
                    fetchModels = { provider, apiKey ->
                        fetchedProvider = provider
                        fetchedApiKey = apiKey
                        listOf(
                            AiClientModelInfo("gpt-4.1", "GPT 4.1", supportsToolCalling = true),
                            AiClientModelInfo("gpt-5", "GPT 5", supportsToolCalling = true),
                        )
                    },
                )
            }
        }

        onNodeWithTag(TestTags.AI_FETCH_MODELS_BUTTON).performClick()
        waitForIdle()

        assertEquals(AiProvider.OpenAi, fetchedProvider)
        assertEquals("sk-test", fetchedApiKey)
        assertEquals("gpt-5", viewModel.getResolvedValue(SettingsKey.AI_MODEL))
    }

    @Test
    fun `shows env var override label and fetch failure`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(
                    viewModel = viewModel,
                    environmentVariables = mapOf("OPENAI_API_KEY" to "sk-env"),
                    fetchModels = { _, _ -> error("network unavailable") },
                )
            }
        }

        onNodeWithText("Using OPENAI_API_KEY from environment").assertExists()
        onNodeWithTag(TestTags.AI_FETCH_MODELS_BUTTON).performClick()
        waitForIdle()

        onNodeWithText("network unavailable").assertExists()
    }

    private fun createViewModel(): SettingsViewModel {
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, PersistenceServiceInMemoryFake())
        manager.loadSettingsAtStartup()
        return SettingsViewModel(manager)
    }
}
