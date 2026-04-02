package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class GridNavTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val focusRequester: FocusRequester,
    val activationService: PlatformActivationServiceInMemoryFake,
)

@OptIn(ExperimentalTestApi::class)
object GridNavTestScenarios {

    private fun createTestComponents(
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): GridNavTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, activationService)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        val focusRequester = FocusRequester()
        return GridNavTestComponents(viewModel, settingsViewModel, focusRequester, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: GridNavTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
            )
        }
    }

    // --- Environment title displayed ---

    fun selectedEnvironmentShowsAsTitleInGridHeader() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Default environment is "all" which has name "All"
        onNodeWithTag(TestTags.GRID_ENVIRONMENT_TITLE)
            .assertExists()
            .assertTextEquals("All")
    }

    // --- Environment title follows dropdown changes ---

    fun gridTitleUpdatesWhenEnvironmentDropdownChanges() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("prod")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_ENVIRONMENT_TITLE)
            .assertExists()
            .assertTextEquals("Production")
    }

    // --- Activator visibility by environment ---

    fun gridCellsShowAndHideBasedOnSelectedEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // In "all" env: app1/res1, app1/res2, app2/res1 have activators; app2/res2 does not
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res1"), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res2"), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app2", "res1"), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app2", "res2"), useUnmergedTree = true).assertDoesNotExist()

        // Switch to "preprod" env: only app1/res1 has an activator
        components.viewModel.selectEnvironment("preprod")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res1"), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res2"), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app2", "res1"), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app2", "res2"), useUnmergedTree = true).assertDoesNotExist()
    }

    // --- Cell click activation ---

    fun cellClickActivatesPreProdResource() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("preprod")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/preprod/app1/dash"))
    }

    fun cellClickActivatesProdResource() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("prod")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/prod/app1/dash"))
    }

    // --- Column click activation ---
    // Column header clicks go through GridNavAppColumnHeadingHoverDetectionOverlay which uses
    // coordinate-based parallelogram hit testing. CMP UI test's performClick cannot reliably
    // target the overlay, so we exercise activation through the ViewModel directly.

    fun columnHeaderClickActivatesAllResourcesInColumn() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // In "all" env, app1 has activators for res1 and res2
        components.viewModel.activateColumn("app1")
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/logs"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Row label click activation ---

    fun rowLabelClickActivatesAllAppsInRow() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // In "all" env, res1 has activators for app1 and app2
        onNodeWithTag(TestTags.gridRowLabel("res1")).performClick()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app2/dash"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Shift-select multi activation ---
    // Shift key detection uses onKeyEvent which CMP UI tests cannot trigger.
    // Exercise multi-select through ViewModel directly.

    fun shiftSelectActivatesMultipleCellsOnRelease() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Toggle-select two cells in "all" env
        components.viewModel.toggleMultiSelect(Coordinate("all", "app1", "res1"))
        components.viewModel.toggleMultiSelect(Coordinate("all", "app2", "res1"))
        waitForIdle()

        // Simulate shift release — triggers multi-activation
        components.viewModel.activateMultiSelect()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/all/app2/dash"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Dropdowns reset after grid activation ---

    fun keyNavDropdownsResetToDefaultsAfterGridActivation() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Set keynav selections to non-default values
        components.viewModel.selectEnvironment("prod")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        // Activate via grid cell click
        components.viewModel.activateCell(Coordinate("prod", "app1", "res1"))
        waitForIdle()

        // Keynav selections should reset: env back to "all", app/resource cleared
        assertEquals("all", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
    }

    // --- Wildcard environment ---

    fun wildcardCellsShowInAllEnvironments() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.WILDCARD_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Default env is "dev" (first in list, no "all" declared)
        assertEquals("dev", components.viewModel.selectedEnvironmentId)

        // Wildcard (app1, res1) should show in dev
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res1"), useUnmergedTree = true)
            .assertExists()

        // Switch to prod — wildcard cell should still show
        components.viewModel.selectEnvironment("prod")
        waitForIdle()
        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res1"), useUnmergedTree = true)
            .assertExists()
    }

    fun wildcardCellActivationWorksAcrossEnvironments() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.WILDCARD_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Activate wildcard cell in dev
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()
        assertEquals(1, components.activationService.openedUrls.size)
        assertEquals("https://example.com/app1/dash", components.activationService.openedUrls.first())

        // Switch to prod and activate same cell
        components.viewModel.selectEnvironment("prod")
        waitForIdle()
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()
        assertEquals(2, components.activationService.openedUrls.size)
        assertEquals("https://example.com/app1/dash", components.activationService.openedUrls[1])
    }
}
