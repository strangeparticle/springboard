package com.strangeparticle.springboard.app.acceptance

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
    val activationService: PlatformActivationServiceInMemoryFake,
)

@OptIn(ExperimentalTestApi::class)
object KeyNavTestScenarios {

    private fun createTestComponents(
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): KeyNavTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        val settingsViewModel = SettingsViewModel(settingsManager)
        return KeyNavTestComponents(viewModel, settingsViewModel, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: KeyNavTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
            )
        }
    }

    // --- Focus on startup ---

    fun appDropdownGetsFocusOnStartup() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // Select a full coordinate: common / app1 / res1
        components.viewModel.selectEnvironment("common")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        // Activate via ViewModel (simulates keyNav Enter)
        components.viewModel.activateCurrentSelection()
        waitForIdle()

        // After activation, selections should reset: env back to first declared, app/resource cleared.
        assertEquals("common", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
        assertTrue(components.activationService.openedUrls.isNotEmpty())
    }

    // --- Environment defaults ---

    fun environmentDefaultsToFirstDeclaredEnvironment() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")

        assertEquals("common", viewModel.selectedEnvironmentId)
    }

    fun environmentDefaultsToFirstDeclaredEnvironmentInTwoEnvFixture() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITHOUT_COMMON, "/test/springboard.json")

        val expected = viewModel.springboard?.environments?.firstOrNull()?.id
        assertEquals(expected, viewModel.selectedEnvironmentId)
    }

    // --- Resource disabled/enabled based on app ---

    fun unavailableResourcesAreDisabledForSelectedApp() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()

        // In MULTI_ENV_WITH_COMMON, env "common" + app2 has only res1 (Dashboard), not res2 (Logs)
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()
        components.viewModel.selectApp("app2")
        waitForIdle()

        // app2 in "common" env has activator for res1 (Dashboard) but not res2 (Logs)
        val resourceStates = components.viewModel.resourceEnabledStates
        assertEquals(true, resourceStates["res1"])
        assertEquals(false, resourceStates["res2"])
    }

    // --- Resource selection after app change ---

    fun selectedResourceResetsWhenUnavailableAfterAppChange() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        viewModel.selectEnvironment("common")

        // Select app1 which has res1 and res2 in "common" env
        viewModel.selectApp("app1")
        viewModel.selectResource("res2")
        assertEquals("res2", viewModel.selectedResourceId)

        // Switch to app2 which only has res1 in "common" env. Resource selection is retained;
        // the resource dropdown shows res2 as disabled instead of silently clearing it.
        viewModel.selectApp("app2")
        assertEquals("res2", viewModel.selectedResourceId)
    }

    fun selectedResourceIsRetainedWhenAvailableAfterAppChange() {
        val viewModel = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())
        viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")

        // Select app1 which has res1 in "common" env
        viewModel.selectApp("app1")
        viewModel.selectResource("res1")
        assertEquals("res1", viewModel.selectedResourceId)

        // Switch to app2 which also has res1 in "common" env — res1 stays selected
        viewModel.selectApp("app2")
        assertEquals("res1", viewModel.selectedResourceId)
    }

    // --- KeyNav activation ---

    fun keyNavActivationWorksForPreProdEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("prod")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        components.viewModel.activateCurrentSelection()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/prod/app1/dash"))
    }

    // --- All-envs environment ---

    fun allEnvsActivatorIsActivatableWhenOnlyAppAndResourceSelected() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Clear the auto-selected environment so only app + resource are set.
        components.viewModel.selectEnvironment(null)
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        assertEquals(true, components.viewModel.isActivateEnabled)

        components.viewModel.activateCurrentSelection()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/app1/dash"))
    }

    fun allEnvsActivatorIsNotConfusedWithEnvSpecificActivatorInDropdownStates() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Selecting a specific env should not surface all-envs activators in env-specific dropdown states.
        components.viewModel.selectEnvironment("dev")
        components.viewModel.selectApp("app1")
        waitForIdle()
        // app1's only activator under dev is the all-envs one, which is not env-specific.
        assertEquals(false, components.viewModel.resourceEnabledStates["res1"])

        // app2 has a prod-specific activator — should be enabled in prod, not in dev.
        components.viewModel.selectApp("app2")
        components.viewModel.selectEnvironment("prod")
        waitForIdle()
        assertEquals(true, components.viewModel.resourceEnabledStates["res2"])
    }

    fun changingEnvironmentKeepsAppAndResourceWhenStillValid() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // Select a full coordinate in "common", then switch environment to one that still
        // supports the same coordinate.
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        components.viewModel.selectEnvironment("preprod")
        waitForIdle()

        assertEquals("preprod", components.viewModel.selectedEnvironmentId)
        assertEquals("app1", components.viewModel.selectedAppId)
        assertEquals("res1", components.viewModel.selectedResourceId)
    }

    fun environmentOptionsAreFilteredBySelectedAppAndResource() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // app2 + res1 exists only in "common" for this fixture.
        components.viewModel.selectApp("app2")
        components.viewModel.selectResource("res1")
        waitForIdle()

        assertEquals(true, components.viewModel.environmentEnabledStates["common"])
        assertEquals(false, components.viewModel.environmentEnabledStates["preprod"])
        assertEquals(false, components.viewModel.environmentEnabledStates["prod"])
    }

    fun dropdownsIncludeNoneOptionToClearSelection() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectApp("app1")
        waitForIdle()

        onNodeWithTag(TestTags.APP_DROPDOWN).performClick()
        waitForIdle()
        onAllNodes(hasText("None") and hasClickAction()).assertCountEquals(1)
        onAllNodes(hasText("None") and hasClickAction())[0].performClick()
        waitForIdle()

        assertNull(components.viewModel.selectedAppId)
    }

    fun environmentDropdownIncludesNoneOption() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).performClick()
        waitForIdle()
        onAllNodes(hasText("None") and hasClickAction()).assertCountEquals(1)
    }

    fun environmentDropdownNoneClearsOnlyEnvironmentSelection() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        components.viewModel.selectEnvironment("common")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).performClick()
        waitForIdle()
        onAllNodes(hasText("None") and hasClickAction()).assertCountEquals(1)
        onAllNodes(hasText("None") and hasClickAction())[0].performClick()
        waitForIdle()

        assertNull(components.viewModel.selectedEnvironmentId)
        assertEquals("app1", components.viewModel.selectedAppId)
        assertEquals("res1", components.viewModel.selectedResourceId)
    }

    fun gridIsHiddenWhenEnvironmentSelectionIsNone() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).performClick()
        waitForIdle()
        onAllNodes(hasText("None") and hasClickAction()).assertCountEquals(1)
        onAllNodes(hasText("None") and hasClickAction())[0].performClick()
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("common")).assertDoesNotExist()
    }

    fun typingSelectsEntryWhenDropdownIsOpen() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        components.viewModel.selectApp("app1")
        waitForIdle()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.RESOURCE_DROPDOWN).performClick()
        waitForIdle()

        onAllNodes(isRoot())[0].performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.L)
        }
        waitForIdle()

        assertEquals("res2", components.viewModel.selectedResourceId)
    }

    fun shiftTabMovesBackwardAndWrapsAcrossDropdownSeries() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        waitForIdle()

        // shift-tab from first should wrap to environment.
        onRoot().performKeyInput {
            keyDown(androidx.compose.ui.input.key.Key.ShiftLeft)
            pressKey(androidx.compose.ui.input.key.Key.Tab)
            keyUp(androidx.compose.ui.input.key.Key.ShiftLeft)
        }
        waitForIdle()
        onNodeWithTag(TestTags.ENVIRONMENT_DROPDOWN).assertIsFocused()

        // regular tab from environment should wrap to first (app dropdown).
        onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }
        waitForIdle()
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

    fun shiftTabDoesNotActivateFromEnvironmentDropdown() = runComposeUiTest {
        val activationService = PlatformActivationServiceInMemoryFake()
        val components = createTestComponents(activationService = activationService)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        components.viewModel.selectEnvironment("prod")
        waitForIdle()

        waitForIdle()

        // Move focus app -> environment via wrapping shift-tab.
        onRoot().performKeyInput {
            keyDown(androidx.compose.ui.input.key.Key.ShiftLeft)
            pressKey(androidx.compose.ui.input.key.Key.Tab)
            keyUp(androidx.compose.ui.input.key.Key.ShiftLeft)
        }
        waitForIdle()

        assertTrue(activationService.openedUrls.isEmpty())

        onRoot().performKeyInput {
            keyDown(androidx.compose.ui.input.key.Key.ShiftLeft)
            pressKey(androidx.compose.ui.input.key.Key.Tab)
            keyUp(androidx.compose.ui.input.key.Key.ShiftLeft)
        }
        waitForIdle()

        assertTrue(activationService.openedUrls.isEmpty())
    }

    fun escapeClearsAllSelectionsAndFocusesAppDropdown() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        components.viewModel.selectEnvironment("preprod")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        waitForIdle()

        // Move focus to the environment dropdown (two tabs from app), then press escape.
        onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }
        onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }
        waitForIdle()

        onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Escape) }
        waitForIdle()

        assertEquals("common", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
        onNodeWithTag(TestTags.APP_DROPDOWN).assertIsFocused()
    }

}
