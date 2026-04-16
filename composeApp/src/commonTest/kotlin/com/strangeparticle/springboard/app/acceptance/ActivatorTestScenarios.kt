package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake

private data class ActivatorTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val focusRequester: FocusRequester,
    val activationService: PlatformActivationServiceInMemoryFake,
)

@OptIn(ExperimentalTestApi::class)
object ActivatorTestScenarios {

    private fun createTestComponents(
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): ActivatorTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        val focusRequester = FocusRequester()
        return ActivatorTestComponents(viewModel, settingsViewModel, focusRequester, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: ActivatorTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
            )
        }
    }

    // --- Single URL activation ---

    fun singleUrlActivatorOpensUrlInBrowser() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Click a single cell with a URL activator
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertEquals(1, components.activationService.openedUrls.size)
        assertEquals("https://example.com/all/app1/dash", components.activationService.openedUrls.first())
    }

    // --- Multiple URL activation ---

    fun multipleUrlActivatorsEachOpenInBrowser() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Activate an entire column — app1 in "all" env has res1 and res2
        components.viewModel.activateColumn("app1")
        waitForIdle()

        assertEquals(2, components.activationService.openedUrls.size)
        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/logs"))
    }
}
