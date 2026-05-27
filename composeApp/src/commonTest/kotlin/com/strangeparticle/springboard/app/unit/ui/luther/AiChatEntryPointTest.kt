package com.strangeparticle.springboard.app.unit.ui.luther

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.mutableStateOf
import com.strangeparticle.luther.client.provider.AiProviderRegistry
import com.strangeparticle.luther.client.provider.openai.OpenAiApiKeySetting
import com.strangeparticle.luther.client.provider.openai.OpenAiPreferredModelSetting
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.stubHttpClientForTests
import com.strangeparticle.springboard.app.ui.AppBottomBar
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneState
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
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
    fun `opening configured chat pane focuses chat input`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `tab from chat input focuses keynav app dropdown`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    @Test
    fun `tab from environment dropdown focuses chat input when assistant is open`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
        onNodeWithTag(TestTags.APP_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.RESOURCE_DROPDOWN).assertIsFocused()
        onNodeWithTag(TestTags.RESOURCE_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).assertIsFocused()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `shift tab from app dropdown focuses chat input when assistant is open`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()

        onNodeWithTag(TestTags.APP_DROPDOWN).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `shift tab from chat input focuses environment dropdown when assistant is open`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).assertIsFocused()
    }

    @Test
    fun `tab from environment dropdown wraps to app dropdown when assistant is closed`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
        onNodeWithTag(TestTags.APP_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.RESOURCE_DROPDOWN).assertIsFocused()
        onNodeWithTag(TestTags.RESOURCE_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).assertIsFocused()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    @Test
    fun `toast auto dismiss does not steal focus from open chat input`() = runComposeUiTest {
        val components = createComponents(configureAi = true)
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("keep focus")
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `chat processing moves focus to keynav then restores input focus`() = runComposeUiTest {
        val requestStarted = CompletableDeferred<Unit>()
        val responseAllowed = CompletableDeferred<Unit>()
        val components = createComponents(
            configureAi = true,
            httpClient = HttpClient(MockEngine {
                requestStarted.complete(Unit)
                responseAllowed.await()
                respond(
                    content = """
                        {
                          "choices": [
                            {
                              "message": { "role": "assistant", "content": "Done" },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                )
            }),
        )
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showFileOpen = false,
            )
        }
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("Add Chrome")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        waitUntil { requestStarted.isCompleted }
        waitForIdle()
        onNodeWithTag(TestTags.AI_CHAT_WORKING_INDICATOR).assertExists()
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()

        responseAllowed.complete(Unit)
        waitUntil { onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_WORKING_INDICATOR).assertDoesNotExist()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `plus button and assistant-created tabs share sequential untitled labels`() = runComposeUiTest {
        val components = createComponents()
        val showAssistant = mutableStateOf(false)
        val aiChatPaneState = AiChatPaneState.configured(
            providerLabel = "Test",
            modelLabel = "test-model",
            transcriptParts = emptyList(),
            onSubmit = { components.viewModel.createUnsavedSpringboardTab() },
            onStop = {},
            onApprovalDecision = { _, _ -> },
        )
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showAssistant = showAssistant,
                aiChatPaneState = aiChatPaneState,
                showFileOpen = false,
            )
        }

        onNodeWithText("Untitled-1").assertExists()
        onNodeWithTag(TestTags.TAB_NEW_BUTTON).performClick()
        onNodeWithText("Untitled-2").assertExists()

        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("Create a new springboard")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        waitUntil { components.viewModel.tabs.size == 3 }
        waitForIdle()

        onNodeWithText("Untitled-3").assertExists()
        assertEquals(
            listOf("Untitled-1", "Untitled-2", "Untitled-3"),
            components.viewModel.tabs.map { it.label },
        )
        assertEquals("Untitled-3", components.viewModel.activeTab?.springboardFilteredForRuntime?.name)
    }

    private fun createComponents(
        configureAi: Boolean = false,
        httpClient: HttpClient = stubHttpClientForTests(),
    ): Components {
        val persistenceService = PersistenceServiceInMemoryFake()
        val registry = SettingsRegistry(
            coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() }
        )
        val settingsManager = SettingsManager(RuntimeEnvironment.DesktopOsx, registry, persistenceService)
        if (configureAi) {
            settingsManager.setUserSetting(AiProviderSetting, "openai")
            settingsManager.setUserSetting(OpenAiApiKeySetting, "test-key")
            settingsManager.setUserSetting(OpenAiPreferredModelSetting, "gpt-5")
        }
        settingsManager.loadSettingsAtStartup()
        return Components(
            viewModel = SpringboardViewModel(
                settingsManager = settingsManager,
                persistenceService = persistenceService,
                platformActivationService = PlatformActivationServiceInMemoryFake(),
            ),
            settingsViewModel = SettingsViewModel(settingsManager, httpClient),
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
}
