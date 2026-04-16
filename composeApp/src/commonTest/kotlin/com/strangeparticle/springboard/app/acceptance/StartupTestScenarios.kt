package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.AppVersion
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake

@OptIn(ExperimentalTestApi::class)
object StartupTestScenarios {

    private fun createTestComponents(): Pair<SpringboardViewModel, SettingsViewModel> {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake())
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        return viewModel to settingsViewModel
    }

    fun springboardIconIsShownInUpperLeft() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        onNodeWithTag(TestTags.SPRINGBOARD_ICON).assertExists().assertIsDisplayed()
    }

    fun versionIdentifierDisplaysCorrectVersion() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        onNodeWithTag(TestTags.VERSION_DESIGNATOR)
            .assertExists()
            .assertTextEquals("v${AppVersion.VERSION}")
    }

    fun springboardWithCommandActivatorsShowsSecurityWarning() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        waitForIdle()
        viewModel.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/test/commands.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Warning"))
            .assertExists()
    }

    fun springboardWithoutCommandActivatorsSkipsSecurityWarning() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        waitForIdle()
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/urls.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Warning"))
            .assertDoesNotExist()
    }

    fun statusLineShowsEmbeddedPathForBuiltInSpringboard() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        waitForIdle()
        val embeddedSource = "(embedded)"
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, embeddedSource)
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains(embeddedSource, substring = true)
    }

    fun statusLineShowsFilePathForCustomLoadedSpringboard() = runComposeUiTest {
        val (viewModel, settingsViewModel) = createTestComponents()
        setContent {
            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        waitForIdle()
        val customPath = "/Users/test/my-springboard.json"
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, customPath)
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains(customPath, substring = true)
    }
}
