package com.strangeparticle.springboard.app.unit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
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
    fun `renders provider dropdown and single api key field`() = runComposeUiTest {
        val viewModel = createViewModel()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_PROVIDER_DROPDOWN).assertExists()
        onNodeWithTag(TestTags.AI_API_KEY_FIELD).assertExists()
    }

    @Test
    fun `api key field is disabled when provider is None`() = runComposeUiTest {
        val viewModel = createViewModel()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_API_KEY_FIELD).assertIsNotEnabled()
    }

    @Test
    fun `api key field becomes enabled after selecting a provider`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_API_KEY_FIELD).assertIsEnabled()
    }

    @Test
    fun `auto-fetches models when provider and key are present, stores preferred`() = runComposeUiTest {
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

        // The LaunchedEffect fires on first composition because provider+key are already set.
        waitForIdle()

        assertEquals(AiProvider.OpenAi, fetchedProvider)
        assertEquals("sk-test", fetchedApiKey)
        assertEquals("gpt-5", viewModel.getResolvedValue(SettingsKey.AI_MODEL))
    }

    @Test
    fun `refresh button re-fetches models`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        var fetchCount = 0

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(
                    viewModel = viewModel,
                    fetchModels = { _, _ ->
                        fetchCount += 1
                        listOf(AiClientModelInfo("gpt-5", "GPT 5", supportsToolCalling = true))
                    },
                )
            }
        }
        waitForIdle()
        val countAfterAutoFetch = fetchCount

        onNodeWithTag(TestTags.AI_REFRESH_MODELS_BUTTON).performClick()
        waitForIdle()

        assertEquals(countAfterAutoFetch + 1, fetchCount)
    }

    @Test
    fun `shows env var override label and surfaces fetch failure`() = runComposeUiTest {
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
        waitForIdle()

        onNodeWithText("Using OPENAI_API_KEY from environment").assertExists()
        onNodeWithText("network unavailable").assertExists()
    }

    @Test
    fun `AI settings have individual clear controls`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        viewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_MODEL_CLEAR_BUTTON).performClick()
        assertEquals("", viewModel.getResolvedValue(SettingsKey.AI_MODEL))

        onNodeWithTag(TestTags.AI_API_KEY_CLEAR_BUTTON).performClick()
        assertEquals("", viewModel.getResolvedValue(SettingsKey.AI_OPENAI_API_KEY))

        onNodeWithTag(TestTags.AI_PROVIDER_CLEAR_BUTTON).performClick()
        assertEquals(AiProvider.None.id, viewModel.getResolvedValue(SettingsKey.AI_PROVIDER))
    }

    @Test
    fun `api key clear control clears persisted key even when env override is active`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(
                    viewModel = viewModel,
                    environmentVariables = mapOf("OPENAI_API_KEY" to "sk-env"),
                    fetchModels = { _, _ -> emptyList() },
                )
            }
        }
        waitForIdle()

        onNodeWithTag(TestTags.AI_API_KEY_CLEAR_BUTTON).performClick()

        assertEquals("", viewModel.getResolvedValue(SettingsKey.AI_OPENAI_API_KEY))
    }

    @Test
    fun `api key visibility toggle switches between hidden and visible`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_API_KEY_VISIBILITY_BUTTON).assertTextEquals("Show")
        onNodeWithTag(TestTags.AI_API_KEY_VISIBILITY_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_API_KEY_VISIBILITY_BUTTON).assertTextEquals("Hide")
    }

    @Test
    fun `AI setting clear buttons match compact settings clear size`() = runComposeUiTest {
        val viewModel = createViewModel()
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        viewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiSettingsSection(viewModel = viewModel)
            }
        }

        onNodeWithTag(TestTags.AI_PROVIDER_CLEAR_BUTTON).assertHeightIsEqualTo(28.dp).assertWidthIsEqualTo(28.dp)
        onNodeWithTag(TestTags.AI_API_KEY_CLEAR_BUTTON).assertHeightIsEqualTo(28.dp).assertWidthIsEqualTo(28.dp)
        onNodeWithTag(TestTags.AI_MODEL_CLEAR_BUTTON).assertHeightIsEqualTo(28.dp).assertWidthIsEqualTo(28.dp)
        onNode(hasContentDescription("Clear AI provider"), useUnmergedTree = true)
            .assertHeightIsEqualTo(14.dp)
            .assertWidthIsEqualTo(14.dp)
        onNode(hasContentDescription("Clear AI API key"), useUnmergedTree = true)
            .assertHeightIsEqualTo(14.dp)
            .assertWidthIsEqualTo(14.dp)
        onNode(hasContentDescription("Clear AI model"), useUnmergedTree = true)
            .assertHeightIsEqualTo(14.dp)
            .assertWidthIsEqualTo(14.dp)
        onNode(hasContentDescription("Refresh model list"), useUnmergedTree = true)
            .assertHeightIsEqualTo(14.dp)
            .assertWidthIsEqualTo(14.dp)
    }

    @Test
    fun `restore defaults clears AI settings`() = runComposeUiTest {
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, PersistenceServiceInMemoryFake())
        manager.loadSettingsAtStartup()
        val viewModel = SettingsViewModel(manager)
        viewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        viewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        viewModel.clearAllUserSettings()

        assertEquals(AiProvider.None.id, viewModel.getResolvedValue(SettingsKey.AI_PROVIDER))
        assertEquals("", viewModel.getResolvedValue(SettingsKey.AI_OPENAI_API_KEY))
        assertEquals("", viewModel.getResolvedValue(SettingsKey.AI_MODEL))
    }

    private fun createViewModel(): SettingsViewModel {
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, PersistenceServiceInMemoryFake())
        manager.loadSettingsAtStartup()
        return SettingsViewModel(manager)
    }
}
