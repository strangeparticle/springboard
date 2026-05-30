package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.mutator.updateApp
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.ui.gridnav.GridNavSizingConstants
import com.strangeparticle.springboard.app.ui.gridnav.IsInMultiSelectKey
import com.strangeparticle.springboard.app.ui.gridnav.IsRowHighlightedKey
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
        target: RuntimeEnvironment = RuntimeEnvironment.Test,
    ): GridNavTestComponents {
        val settingsManager = createSettingsManagerForTest(target = target)
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        val settingsViewModel = SettingsViewModel(settingsManager, com.strangeparticle.springboard.app.shared.stubHttpClientForTests())
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithContentDescription(HEADER_RESIZE_DRAG_HANDLE_CONTENT_DESCRIPTION)
            .assertExists()
    }

    // --- Header resize thumb presence and drag behavior ---

    fun headerResizeThumbHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).assertExists()
    }

    fun headerResizeGripGlyphHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_GRIP_GLYPH, useUnmergedTree = true)
            .assertExists()
    }

    fun draggingHeaderResizeThumbDownGrowsHeaderHeight() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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

    // --- Header height stability across in-place API mutations (issue #92) ---

    // Renames an app on the active tab the same way the update_app tool call does:
    // apply the pure-function mutator and push the result back via replaceTabSpringboard.
    // This mutates the springboard in place, keeping GridNav composed (the bug only
    // surfaces when GridNav stays composed while its app list changes).
    private fun renameActiveTabApp(components: GridNavTestComponents, appId: String, newName: String) {
        val currentSpringboard = components.viewModel.activeTab!!.springboardUnfiltered!!
        val renamedSpringboard = updateApp(currentSpringboard, appId = appId, newName = newName)
        components.viewModel.replaceTabSpringboard(components.viewModel.activeTabId, renamedSpringboard)
    }

    fun renamingAppViaApiPreservesDraggedHeaderHeight() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // Drag the header to its maximum height so the user's chosen height is taller
        // than any height the renamed app would auto-compute. This isolates the reset
        // bug: the renamed initial height is always below this dragged value.
        val excessivePx = with(density) {
            (GridNavSizingConstants.MaxHeaderHeight + 200.dp).toPx()
        }
        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, excessivePx))
            up()
        }
        waitForIdle()

        val draggedTop = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top

        // Rename an app to a longer name so its auto-computed initial header height
        // changes (the longest name now drives a taller, but still sub-maximum, height).
        renameActiveTabApp(components, appId = "app1", newName = "App One Extended Nme")
        waitForIdle()

        val afterRenameTop = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top
        assertEquals(
            draggedTop,
            afterRenameTop,
            "Expected the dragged header height to survive an API rename, but the thumb moved from $draggedTop to $afterRenameTop",
        )
    }

    fun headerResizeThumbStillRespondsAfterApiRename() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, 60f))
            up()
        }
        waitForIdle()

        renameActiveTabApp(components, appId = "app1", newName = "App One Extended Nme")
        waitForIdle()

        val topBeforeSecondDrag = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top

        onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(0f, 60f))
            up()
        }
        waitForIdle()

        val topAfterSecondDrag = onNodeWithTag(TestTags.GRID_HEADER_RESIZE_THUMB)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            topAfterSecondDrag > topBeforeSecondDrag,
            "Expected the resize thumb to still respond to dragging after an API rename: was $topBeforeSecondDrag, now $topAfterSecondDrag",
        )
    }

    fun columnResizeThumbHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).assertExists()
    }

    fun columnResizeGripGlyphHasTestTag() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_GRIP_GLYPH, useUnmergedTree = true)
            .assertExists()
    }

    fun draggingColumnResizeThumbRightGrowsResourceLabelColumn() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        val initialWidth = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(40f, 0f))
            up()
        }
        waitForIdle()

        val newWidth = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            newWidth > initialWidth,
            "Expected resource label width to increase after dragging right: was $initialWidth, now $newWidth",
        )
    }

    fun draggingColumnResizeThumbLeftShrinksResourceLabelColumn() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(60f, 0f))
            up()
        }
        waitForIdle()

        val afterGrow = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(-40f, 0f))
            up()
        }
        waitForIdle()

        val afterShrink = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            afterShrink < afterGrow,
            "Expected resource label width to decrease after dragging left: was $afterGrow, now $afterShrink",
        )
    }

    fun draggingColumnResizeThumbClampsAtMaxWidth() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        val excessivePx = with(density) {
            (GridNavSizingConstants.MaxResourceLabelWidth + 200.dp).toPx()
        }
        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(excessivePx, 0f))
            up()
        }
        waitForIdle()

        val afterFirstDrag = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width

        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(50f, 0f))
            up()
        }
        waitForIdle()

        val afterSecondDrag = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width
        assertEquals(
            afterFirstDrag,
            afterSecondDrag,
            "Expected resource label width to remain clamped after additional right drag",
        )
    }

    fun draggingColumnResizeThumbClampsAtMinWidth() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        val initialWidth = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width

        val excessivePx = with(density) {
            (CommonUiConstants.ResourceLabelWidth + 200.dp).toPx()
        }
        onNodeWithTag(TestTags.GRID_COLUMN_RESIZE_THUMB).performTouchInput {
            down(center)
            moveBy(Offset(-excessivePx, 0f))
            up()
        }
        waitForIdle()

        val afterFirstDrag = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode().boundsInRoot.width
        assertEquals(
            initialWidth,
            afterFirstDrag,
            "Expected resource label width not to shrink below the default heading-safe width",
        )
    }

    fun selectedEnvironmentShowsAsTitleInGridHeader() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // Default environment is the first in the list ("common" with name "Common" in this fixture)
        onNodeWithTag(TestTags.gridSectionHeading("common"))
            .assertExists()
            .assertTextEquals("Common")
    }

    // --- Environment title follows dropdown changes ---

    fun gridTitleUpdatesWhenEnvironmentDropdownChanges() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("prod")
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("prod"))
            .assertExists()
            .assertTextEquals("Production")
    }

    fun gridEnvironmentHeadingDropdownChangesEnvironmentAndClearsKeyNavSelections() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        components.viewModel.selectApp("app1")
        components.viewModel.selectResource("res1")
        waitForIdle()

        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(CommonUiConstants.ToastAutoDismissMs + 500)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("common")).performClick()
        waitForIdle()
        onAllNodes(hasText("Production") and hasClickAction())[0].performClick()
        waitForIdle()

        assertEquals("prod", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
        onNodeWithTag(TestTags.gridSectionHeading("prod"))
            .assertExists()
            .assertTextEquals("Production")
    }

    // --- Activator visibility by environment ---

    fun gridCellsShowAndHideBasedOnSelectedEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // In "common" env: app1/res1, app1/res2, app2/res1 have activators; app2/res2 does not
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment("preprod")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/preprod/app1/dash"))
    }

    fun wasmCommandActivatorCellIsHiddenAndCannotExecute() = runComposeUiTest {
        val components = createTestComponents(target = RuntimeEnvironment.WASM)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/test/commands.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCellActivatorIndicator("app1", "res1"))
            .assertDoesNotExist()
        components.viewModel.activateCell(Coordinate("dev", "app1", "res1"))

        assertTrue(components.activationService.executedCommands.isEmpty())
        assertNull(components.viewModel.springboardFilteredForRuntime?.indexes?.activatorByCoordinate?.get(Coordinate("dev", "app1", "res1")))
        assertTrue(components.viewModel.springboardUnfiltered?.activators.orEmpty().isNotEmpty())
    }

    fun cellClickActivatesProdResource() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // In "common" env, app1 has activators for res1 and res2
        components.viewModel.activateColumn("common", "app1")
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app1/logs"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Row label click activation ---

    fun rowLabelClickActivatesAllAppsInRow() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // In "common" env, res1 has activators for app1 and app2
        onNodeWithTag(TestTags.gridRowLabel("res1")).performClick()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app2/dash"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Shift-select multi activation ---
    // Shift key detection uses onKeyEvent which CMP UI tests cannot trigger.
    // Exercise multi-select through ViewModel directly.

    fun shiftSelectActivatesMultipleCellsOnRelease() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // Toggle-select two cells in "common" env
        components.viewModel.toggleMultiSelect(Coordinate("common", "app1", "res1"))
        components.viewModel.toggleMultiSelect(Coordinate("common", "app2", "res1"))
        waitForIdle()

        // Simulate shift release — triggers multi-activation
        components.viewModel.activateMultiSelect()
        waitForIdle()

        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app1/dash"))
        assertTrue(components.activationService.openedUrls.contains("https://example.com/common/app2/dash"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // Regression test: while a shift-select is in progress, each cell that has been
    // added to the selection bucket must visually indicate that — not just on
    // shift-release. The cell's activator indicator carries an IsInMultiSelect
    // semantic so the test can verify the visual state tracks the bucket.
    fun shiftSelectedCellsVisuallyIndicateSelection() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        fun cellInMultiSelect(appId: String, resourceId: String): Boolean {
            val config = onNodeWithTag(TestTags.gridCell(appId, resourceId))
                .fetchSemanticsNode().config
            return config.getOrElseNullable(IsInMultiSelectKey) { false } == true
        }

        // Baseline: nothing selected.
        assertEquals(false, cellInMultiSelect("app1", "res1"))
        assertEquals(false, cellInMultiSelect("app2", "res1"))

        // Add app1/res1 to the bucket (no shift release yet); indicator should reflect it.
        components.viewModel.toggleMultiSelect(Coordinate("common", "app1", "res1"))
        waitForIdle()
        assertTrue(cellInMultiSelect("app1", "res1"), "app1/res1 should show as selected after toggle")
        assertEquals(false, cellInMultiSelect("app2", "res1"))

        // Add a second cell.
        components.viewModel.toggleMultiSelect(Coordinate("common", "app2", "res1"))
        waitForIdle()
        assertTrue(cellInMultiSelect("app1", "res1"), "app1/res1 should still be selected")
        assertTrue(cellInMultiSelect("app2", "res1"), "app2/res1 should now be selected")

        // Toggle the first cell off — its indicator should clear, the second stays.
        components.viewModel.toggleMultiSelect(Coordinate("common", "app1", "res1"))
        waitForIdle()
        assertEquals(false, cellInMultiSelect("app1", "res1"))
        assertTrue(cellInMultiSelect("app2", "res1"))
    }

    // --- Dropdowns reset after grid activation ---

    fun keyNavDropdownsResetToDefaultsAfterGridActivation() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
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
        assertEquals("common", components.viewModel.selectedEnvironmentId)
        assertNull(components.viewModel.selectedAppId)
        assertNull(components.viewModel.selectedResourceId)
    }

    // --- All-envs section ---

    fun allEnvsSectionHeadingAppearsAboveAllEnvsResources() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("ALL"), useUnmergedTree = true)
            .assertExists()
        // The all-envs section renders an all-envs cell for app1+res1 (the all-envs activator coordinate).
        onNodeWithTag(TestTags.gridAllEnvsCellActivatorIndicator("app1", "res1"), useUnmergedTree = true)
            .assertExists()
    }

    fun allEnvsSectionIsAbsentWhenSpringboardHasNoAllEnvsActivators() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITHOUT_COMMON, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("ALL"), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    fun allEnvsSectionRendersWhenNoEnvironmentSelected() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        components.viewModel.selectEnvironment(null)
        waitForIdle()

        onNodeWithTag(TestTags.gridSectionHeading("ALL"), useUnmergedTree = true)
            .assertExists()
        onNodeWithTag(TestTags.gridAllEnvsCellActivatorIndicator("app1", "res1"), useUnmergedTree = true)
            .assertExists()
    }

    // --- App groups: visual reordering with separator columns ---

    fun appGroupsRenderColumnsInGroupOrderWithSeparators() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.APP_GROUPS_WITH_SEPARATORS, "/test/springboard.json")
        waitForIdle()

        // All four cells exist regardless of the visual reorder.
        onNodeWithTag(TestTags.gridCell("app1", "res1")).assertExists()
        onNodeWithTag(TestTags.gridCell("app2", "res1")).assertExists()
        onNodeWithTag(TestTags.gridCell("app3", "res1")).assertExists()
        onNodeWithTag(TestTags.gridCell("app4", "res1")).assertExists()

        // Layout slots: [app1, app3, sep, app2, sep, app4] → separators at indices 2 and 4.
        onNodeWithTag(TestTags.gridColumnSeparator(2), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.gridColumnSeparator(4), useUnmergedTree = true).assertExists()

        // Visual ordering: app3 (groupA, second member) sits left of app2 (groupB).
        val app1Left = onNodeWithTag(TestTags.gridCell("app1", "res1"))
            .fetchSemanticsNode().boundsInRoot.left
        val app3Left = onNodeWithTag(TestTags.gridCell("app3", "res1"))
            .fetchSemanticsNode().boundsInRoot.left
        val app2Left = onNodeWithTag(TestTags.gridCell("app2", "res1"))
            .fetchSemanticsNode().boundsInRoot.left
        val app4Left = onNodeWithTag(TestTags.gridCell("app4", "res1"))
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(app1Left < app3Left, "app1 should sit left of app3 (same group)")
        assertTrue(app3Left < app2Left, "app3 (groupA) should sit left of app2 (groupB)")
        assertTrue(app2Left < app4Left, "app2 (groupB) should sit left of app4 (ungrouped tail)")

        // Cell click still activates the matching activator after the visual reorder.
        onNodeWithTag(TestTags.gridCell("app2", "res1")).performClick()
        waitForIdle()
        assertEquals(1, components.activationService.openedUrls.size)
        assertEquals("https://example.com/app2", components.activationService.openedUrls.first())
    }

    fun appGroupsAreAbsentFromLayoutWhenNoneDeclared() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        // No appGroups declared in this fixture — no separator slots should render.
        onNodeWithTag(TestTags.gridColumnSeparator(0), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(TestTags.gridColumnSeparator(1), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(TestTags.gridColumnSeparator(2), useUnmergedTree = true).assertDoesNotExist()
    }

    fun allEnvsCellActivatesAllEnvsActivatorRegardlessOfSelectedEnvironment() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test/springboard.json")
        waitForIdle()

        // Default selected env is the first configured env (dev).
        assertEquals("dev", components.viewModel.selectedEnvironmentId)

        onNodeWithTag(TestTags.gridAllEnvsCell("app1", "res1")).performClick()
        waitForIdle()
        assertEquals(1, components.activationService.openedUrls.size)
        assertEquals("https://example.com/app1/dash", components.activationService.openedUrls.first())

        // Switch env and activate again — the all-envs cell still activates the all-envs activator.
        components.viewModel.selectEnvironment("prod")
        waitForIdle()
        onNodeWithTag(TestTags.gridAllEnvsCell("app1", "res1")).performClick()
        waitForIdle()
        assertEquals(2, components.activationService.openedUrls.size)
        assertEquals("https://example.com/app1/dash", components.activationService.openedUrls[1])
    }

    // Regression test: clicking a column header must trigger activateColumn for that
    // column. The click goes through GridNavAppColumnHeadingHoverDetectionOverlay's
    // parallelogram pointerInput handler. We discover the click position by hovering
    // first (which we already verified works in the row-hover regression test) — when
    // the overlay reports the hovered app, we know the same pointer-x/y will resolve
    // to that app for the Press event too.
    fun clickingColumnHeaderActivatesAllResourcesInColumn() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()
        components.viewModel.selectEnvironment("common")
        waitForIdle()

        // Find the data-cell column for app1 to derive a horizontal x that lies
        // within app1's column. Click at the very bottom edge of the header overlay
        // at that x — the parallelogram skew transform reduces to identity on the
        // bottom edge, so x maps directly to the column.
        val app1Cell = onNodeWithTag(TestTags.gridCell("app1", "res1")).fetchSemanticsNode()
        val app1CenterX = app1Cell.boundsInRoot.left + app1Cell.size.width / 2

        // Click on the header overlay at app1's column-x, inside the active rotated-header
        // area above the reserved group-label strip.
        onRoot().performMouseInput {
            val stripHeightPx = with(density) { GridNavSizingConstants.GroupLabelStripHeight.toPx() }
            val y = app1Cell.boundsInRoot.top - stripHeightPx - 4f
            click(androidx.compose.ui.geometry.Offset(app1CenterX, y))
        }
        waitForIdle()

        assertTrue(
            components.activationService.openedUrls.contains("https://example.com/common/app1/dash"),
            "expected app1/res1 dashboard URL to be opened, got: ${components.activationService.openedUrls}",
        )
        assertTrue(
            components.activationService.openedUrls.contains("https://example.com/common/app1/logs"),
            "expected app1/res2 logs URL to be opened, got: ${components.activationService.openedUrls}",
        )
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // Same regression check as the previous test, but for an app that sits AFTER
    // a separator slot (groupB). With app groups the parallelogram overlay's
    // column-index math has to account for separator slots correctly.
    fun clickingColumnHeaderInAppGroupsLayoutActivatesAllResourcesInColumn() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.APP_GROUPS_WITH_SEPARATORS, "/test/springboard.json")
        waitForIdle()

        // Layout: [app1, app3, sep, app2, sep, app4]. Click on app2 (after a separator).
        val app2Cell = onNodeWithTag(TestTags.gridCell("app2", "res1")).fetchSemanticsNode()
        val app2CenterX = app2Cell.boundsInRoot.left + app2Cell.size.width / 2
        onRoot().performMouseInput {
            val stripHeightPx = with(density) { GridNavSizingConstants.GroupLabelStripHeight.toPx() }
            val y = app2Cell.boundsInRoot.top - stripHeightPx - 4f
            click(androidx.compose.ui.geometry.Offset(app2CenterX, y))
        }
        waitForIdle()

        assertTrue(
            components.activationService.openedUrls.contains("https://example.com/app2"),
            "expected app2/res1 URL to be opened, got: ${components.activationService.openedUrls}",
        )
    }

    fun headerResizeDragStillWorksWithGroupLabelStripPresent() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.APP_GROUPS_WITH_SEPARATORS, "/test/springboard.json")
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
            "Expected resize thumb to move down with group label strip present: was $initialTop, now $newTop",
        )
    }

    // Regression test: hovering a row header label must propagate the row-highlight
    // state into all data cells in that row, not just the header label itself.
    fun hoveringRowHeaderHighlightsCellsInThatRow() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        fun cellHighlighted(appId: String, resourceId: String): Boolean {
            val config = onNodeWithTag(TestTags.gridCell(appId, resourceId))
                .fetchSemanticsNode().config
            return config.getOrElseNullable(IsRowHighlightedKey) { false } == true
        }

        // Baseline: no hover, nothing highlighted.
        assertEquals(false, cellHighlighted("app1", "res1"))
        assertEquals(false, cellHighlighted("app2", "res1"))

        // Move the mouse over the row header for res1; cells in that row should highlight.
        onNodeWithTag(TestTags.gridRowLabel("res1")).performMouseInput {
            moveTo(center)
        }
        waitForIdle()

        assertTrue(cellHighlighted("app1", "res1"), "app1/res1 cell should be row-highlighted")
        assertTrue(cellHighlighted("app2", "res1"), "app2/res1 cell should be row-highlighted")
        // Cells in other rows must not be highlighted.
        assertEquals(false, cellHighlighted("app1", "res2"))
    }

    fun movingPointerFromActivatorCellToRowHeaderClearsActivatorPreview() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test/springboard.json")
        waitForIdle()

        val cellBounds = onNodeWithTag(TestTags.gridCell("app1", "res1"))
            .fetchSemanticsNode()
            .boundsInRoot
        val rowHeaderBounds = onNodeWithTag(TestTags.gridRowLabel("res1"))
            .fetchSemanticsNode()
            .boundsInRoot

        onRoot().performMouseInput {
            moveTo(cellBounds.center)
        }
        waitForIdle()

        assertEquals(
            "url: https://example.com/common/app1/dash",
            components.viewModel.hoveredActivatorPreview,
        )

        onRoot().performMouseInput {
            moveTo(Offset(rowHeaderBounds.center.x, cellBounds.center.y))
        }
        waitForIdle()

        assertNull(components.viewModel.hoveredActivatorPreview)
    }
}
