package com.strangeparticle.springboard.app.unit.ui.editio

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test

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
}
