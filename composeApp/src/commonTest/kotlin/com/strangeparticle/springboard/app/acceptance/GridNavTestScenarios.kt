package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.ui.gridnav.GridNavSizingConstants
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
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake

private data class GridNavTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val activationService: PlatformActivationServiceInMemoryFake,
)

@OptIn(ExperimentalTestApi::class)
object GridNavTestScenarios {

    private const val HEADER_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION = "Header resize drag handle"

    private fun createTestComponents(
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): GridNavTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        return GridNavTestComponents(viewModel, settingsViewModel, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: GridNavTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
            )
        }
    }

    // --- Environment title displayed ---

    fun headerResizeThumbShowsDragHandleIcon() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithContentDescription(HEADER_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION)
            .assertExists()
    }

    // --- Header resize thumb presence and drag behavior ---

    fun headerResizeThumbHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).assertExists()
    }

    fun headerResizeGripGlyphHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_GRIP_GLYPH, useUnmergedTree = true)
            .assertExists()
    }

    fun draggingHeaderResizeThumbDownGrowsHeaderHeight() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        val thumb = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
        val initialTop = thumb.fetchSemanticsNode().boundsInRoot.top

        thumb.performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            up()
        }
        waitForIdle()

        val newTop = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            newTop > initialTop,
            "Expected thumb top to increase after dragging down: was $initialTop, now $newTop",
        )
    }

    fun draggingHeaderResizeThumbUpShrinksHeaderHeight() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // First drag down so we have headroom to shrink back.
        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, 60f))
            up()
        }
        waitForIdle()

        val afterGrow = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, -40f))
            up()
        }
        waitForIdle()

        val afterShrink = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            afterShrink < afterGrow,
            "Expected thumb top to decrease after dragging up: was $afterGrow, now $afterShrink",
        )
    }

    fun draggingHeaderResizeThumbClampsAtMaxHeight() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Drag well past the maximum bound.
        val excessivePx = with(density) {
            (GridNavSizingConstants.MaxHeaderHeight + 200.dp).toPx()
        }
        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, excessivePx))
            up()
        }
        waitForIdle()

        val afterFirstDrag = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top

        // A second downward drag should not move the thumb further: already clamped.
        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, 50f))
            up()
        }
        waitForIdle()

        val afterSecondDrag = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top
        assertEquals(
            afterFirstDrag,
            afterSecondDrag,
            "Expected thumb position to remain clamped after additional downward drag",
        )
    }

    fun selectedEnvironmentShowsAsTitleInGridHeader() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Default environment is the first in the list ("all" with name "All" in this fixture)
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

    // --- Guidance marker visibility ---

    fun guidanceMarkerAppearsForCellsWithGuidance() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_GUIDANCE, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellGuidanceIndicator("app1", "res1"), useUnmergedTree = true).assertExists()
    }

    fun guidanceMarkerAbsentForCellsWithoutGuidance() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_GUIDANCE, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellGuidanceIndicator("app1", "res2"), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(TestTags.gridCellGuidanceIndicator("app2", "res1"), useUnmergedTree = true).assertDoesNotExist()
    }

    fun guidanceMarkerVisibilityFollowsEnvironmentSelection() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_GUIDANCE, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellGuidanceIndicator("app1", "res1"), useUnmergedTree = true).assertExists()

        components.viewModel.selectEnvironment("preprod")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellGuidanceIndicator("app1", "res1"), useUnmergedTree = true).assertDoesNotExist()
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

        // Keynav selections should reset: env back to default (first in list), app/resource cleared.
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

        // Default env is the first one in the list.
        assertEquals("dev", components.viewModel.selectedEnvironmentId)

        components.viewModel.selectEnvironment("dev")
        waitForIdle()

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
