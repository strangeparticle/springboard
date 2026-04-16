package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake

private data class KeyNavTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val focusRequester: FocusRequester,
    val activationService: PlatformActivationServiceInMemoryFake,
)

@OptIn(ExperimentalTestApi::class)
object KeyNavTestScenarios {

    private fun createTestComponents(
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): KeyNavTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        val focusRequester = FocusRequester()
        return KeyNavTestComponents(viewModel, settingsViewModel, focusRequester, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: KeyNavTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
                onRequestFocusFirstDropdown = {
                    try {
                        components.focusRequester.requestFocus()
                    } catch (_: Exception) { }
                },
            )
        }
    }

    // --- Focus on startup ---

    fun appDropdownGetsFocusOnStartup() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.focusRequester.requestFocus()
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    // --- Focus after toast ---

    fun appDropdownGetsFocusAfterToastAutoCloses() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()

        // loadConfig emits an INFO toast ("Springboard loaded: ...") which auto-dismisses
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        mainClock.autoAdvance = false
        // Advance past the auto-dismiss timeout
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    fun appDropdownGetsFocusAfterToastIsManuallyClosed() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()

        // loadConfig with a command activator triggers a WARNING toast (persists until dismissed)
        // and an INFO toast ("Springboard loaded") which auto-dismisses.
        components.viewModel.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/test/springboard.json")
        waitForIdle()

        // Wait for the INFO toast to auto-dismiss so only the WARNING remains
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        // Dismiss the remaining WARNING toast manually
        onNodeWithTag(TestTags.TOAST_DISMISS_BUTTON).performClick()
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    // --- Dropdowns reset after activation ---

    fun dropdownsResetToDefaultsAfterKeyNavActivation() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Select a full coordinate: all / app1 / res1
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        // Activate via ViewModel (simulates keyNav Enter)
        components.viewModel.activateCurrentSelection()
        waitForIdle()

        // After activation, selections should reset: env back to "all", app/resource cleared
        assertEquals("all", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
        assertTrue(components.activationService.openedUrls.isNotEmpty())
    }

    // --- Environment defaults ---

    fun environmentDefaultsToAllWhenPresent() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")

        assertEquals("all", viewModel.selectedEnvironmentId)
    }

    fun environmentDefaultsToFirstWhenAllIsNotPresent() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITHOUT_ALL, "/test/springboard.json")

        assertEquals("preprod", viewModel.selectedEnvironmentId)
    }

    // --- Resource disabled/enabled based on app ---

    fun unavailableResourcesAreDisabledForSelectedApp() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()

        // In MULTI_ENV_WITH_ALL, env "all" + app2 has only res1 (Dashboard), not res2 (Logs)
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()
        components.viewModel.selectApp("app2")
        waitForIdle()

        // app2 in "all" env has activator for res1 (Dashboard) but not res2 (Logs)
        val resourceStates = components.viewModel.resourceEnabledStates
        assertEquals(true, resourceStates["res1"])
        assertEquals(false, resourceStates["res2"])
    }

    // --- Resource selection after app change ---

    fun selectedResourceResetsWhenUnavailableAfterAppChange() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")

        // Select app1 which has res1 and res2 in "all" env
        viewModel.selectApp("app1")
        viewModel.selectResource("res2")
        assertEquals("res2", viewModel.selectedResourceId)

        // Switch to app2 which only has res1 in "all" env — res2 becomes unavailable
        viewModel.selectApp("app2")
        assertNull(viewModel.selectedResourceId)
    }

    fun selectedResourceIsRetainedWhenAvailableAfterAppChange() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")

        // Select app1 which has res1 in "all" env
        viewModel.selectApp("app1")
        viewModel.selectResource("res1")
        assertEquals("res1", viewModel.selectedResourceId)

        // Switch to app2 which also has res1 in "all" env — res1 stays selected
        viewModel.selectApp("app2")
        assertEquals("res1", viewModel.selectedResourceId)
    }

    // --- KeyNav activation ---

    fun keyNavActivationWorksForPreProdEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("preprod")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        components.viewModel.activateCurrentSelection()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/preprod/app1/dash"))
    }

    fun keyNavActivationWorksForProdEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("prod")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        components.viewModel.activateCurrentSelection()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/prod/app1/dash"))
    }

    // --- Wildcard environment ---

    fun wildcardActivatorAppIsEnabledForAllEnvironments() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.WILDCARD_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // In dev env, app1 should be enabled (wildcard activator)
        components.viewModel.selectEnvironment("dev")
        waitForIdle()
        assertEquals(true, components.viewModel.appEnabledStates["app1"])

        // In prod env, app1 should still be enabled (wildcard activator)
        components.viewModel.selectEnvironment("prod")
        waitForIdle()
        assertEquals(true, components.viewModel.appEnabledStates["app1"])
    }

    fun wildcardActivatorResourceIsEnabledAfterSelectingApp() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.WILDCARD_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Select dev env and app1 — res1 should be enabled (wildcard activator)
        components.viewModel.selectEnvironment("dev")
        components.viewModel.selectApp("app1")
        waitForIdle()
        assertEquals(true, components.viewModel.resourceEnabledStates["res1"])

        // Switch to prod — res1 should still be enabled
        components.viewModel.selectEnvironment("prod")
        components.viewModel.selectApp("app1")
        waitForIdle()
        assertEquals(true, components.viewModel.resourceEnabledStates["res1"])
    }
}
