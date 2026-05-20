package com.strangeparticle.springboard.app.unit.ui.editio

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.client.AiClientResponse
import com.strangeparticle.editio.client.AiClientStopReason
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.editio.help.AiAssistantFullHelpText
import com.strangeparticle.springboard.app.ui.AppBottomBar
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class AiChatEntryPointTest {

    @Test
    fun `assistant icon opens chat pane`() = runComposeUiTest {
        val components = createComponents()
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.AI_CHAT_PANE).assertDoesNotExist()
        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_PANE).assertExists()
    }

    @Test
    fun `assistant icon changes color while chat pane is open`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AppBottomBar(
                    isAssistantConfigured = true,
                    isAssistantOpen = false,
                    onToggleAssistant = {},
                    onOpenSettings = {},
                )
            }
        }

        val closedPixels = onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).captureToImage().toPixelMap()

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AppBottomBar(
                    isAssistantConfigured = true,
                    isAssistantOpen = true,
                    onToggleAssistant = {},
                    onOpenSettings = {},
                )
            }
        }

        val openPixels = onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).captureToImage().toPixelMap()

        assertTrue(
            pixelSignature(closedPixels) != pixelSignature(openPixels),
            "assistant icon should visibly change color while chat pane is open",
        )
    }

    @Test
    fun `chat pane sits between tab bar and bottom bar`() = runComposeUiTest {
        val components = createComponents()
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()

        val chatBounds = onNodeWithTag(TestTags.AI_CHAT_PANE).getUnclippedBoundsInRoot()
        val tabBarBounds = onNodeWithTag(TestTags.TAB_BAR).getUnclippedBoundsInRoot()
        assertTrue(
            chatBounds.top >= tabBarBounds.bottom,
            "chat pane must be laid out below the tab bar (which sits between the status bar and the chat pane)",
        )
    }

    @Test
    fun `chat settings action opens settings screen`() = runComposeUiTest {
        val components = createComponents()
        val showSettings = mutableStateOf(false)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showSettings = showSettings,
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_SETTINGS_BUTTON).performClick()
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertExists()
    }

    @Test
    fun `configured AI settings show chat input instead of not configured prompt`() = runComposeUiTest {
        val components = createComponents()
        components.settingsViewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        components.settingsViewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        components.settingsViewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                aiClientFactory = { _, _ -> AiClientNoopFake() },
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertExists()
        onNodeWithText("AI is not configured").assertDoesNotExist()
    }

    @Test
    fun `configured AI settings without a client remain not configured`() = runComposeUiTest {
        val components = createComponents()
        components.settingsViewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        components.settingsViewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        components.settingsViewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                aiClientFactory = { _, _ -> null },
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()

        onNodeWithText("AI is not configured").assertExists()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertDoesNotExist()
    }

    @Test
    fun `help slash command renders locally without calling AI client`() = runComposeUiTest {
        val components = createComponents()
        val aiClient = AiClientCountingFake()
        components.settingsViewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        components.settingsViewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        components.settingsViewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                aiClientFactory = { _, _ -> aiClient },
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("/help")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        onNodeWithText("You: /help").assertExists()
        onNodeWithText(AiAssistantFullHelpText.title, substring = true).assertExists()
        onNodeWithTag(TestTags.AI_CHAT_HISTORY).performScrollToIndex(0)
        onNodeWithText("/help_terse").assertExists()
        assertEquals(0, aiClient.requestCount)
    }

    @Test
    fun `terse help slash command renders locally without calling AI client`() = runComposeUiTest {
        val components = createComponents()
        val aiClient = AiClientCountingFake()
        components.settingsViewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        components.settingsViewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        components.settingsViewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                aiClientFactory = { _, _ -> aiClient },
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("/help_terse")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        onNodeWithText("You: /help_terse").assertExists()
        assertEquals(0, aiClient.requestCount)
    }

    @Test
    fun `unknown slash command renders local error without calling AI client`() = runComposeUiTest {
        val components = createComponents()
        val aiClient = AiClientCountingFake()
        components.settingsViewModel.setUserSetting(SettingsKey.AI_PROVIDER, AiProvider.OpenAi.id)
        components.settingsViewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, "sk-test")
        components.settingsViewModel.setUserSetting(SettingsKey.AI_MODEL, "gpt-5")

        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                aiClientFactory = { _, _ -> aiClient },
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("/wat")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        onNodeWithText("You: /wat").assertExists()
        onNodeWithText("Unknown command: /wat. Try /help.").assertExists()
        assertEquals(0, aiClient.requestCount)
    }

    private fun createComponents(): Components {
        val persistenceService = PersistenceServiceInMemoryFake()
        val settingsManager = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager.loadSettingsAtStartup()
        return Components(
            viewModel = SpringboardViewModel(
                settingsManager = settingsManager,
                persistenceService = persistenceService,
                platformActivationService = PlatformActivationServiceInMemoryFake(),
            ),
            settingsViewModel = SettingsViewModel(settingsManager),
        )
    }

    private data class Components(
        val viewModel: SpringboardViewModel,
        val settingsViewModel: SettingsViewModel,
    )

    private fun pixelSignature(pixelMap: androidx.compose.ui.graphics.PixelMap): Int {
        var signature = 0
        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                signature = 31 * signature + pixelMap[x, y].hashCode()
            }
        }
        return signature
    }

    private class AiClientNoopFake : AiClient {
        override suspend fun sendAiRequest(request: AiClientRequest): AiClientResponse =
            AiClientResponse(
                text = "ok",
                toolCalls = emptyList(),
                stopReason = AiClientStopReason.Stop,
                raw = buildJsonObject {},
            )

        override suspend fun listModels(apiKey: String): List<AiClientModelInfo> = emptyList()
    }

    private class AiClientCountingFake : AiClient {
        var requestCount = 0
            private set

        override suspend fun sendAiRequest(request: AiClientRequest): AiClientResponse {
            requestCount += 1
            return AiClientResponse(
                text = "ok",
                toolCalls = emptyList(),
                stopReason = AiClientStopReason.Stop,
                raw = buildJsonObject {},
            )
        }

        override suspend fun listModels(apiKey: String): List<AiClientModelInfo> = emptyList()
    }
}
