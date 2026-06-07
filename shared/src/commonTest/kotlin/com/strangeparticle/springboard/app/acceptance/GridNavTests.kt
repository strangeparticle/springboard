package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class GridNavTests {

    @Test fun `header resize thumb shows drag handle icon`() =
        GridNavTestScenarios.headerResizeThumbShowsDragHandleIcon()

    @Test fun `header resize thumb has test tag`() =
        GridNavTestScenarios.headerResizeThumbHasTestTag()

    @Test fun `header resize grip glyph has test tag`() =
        GridNavTestScenarios.headerResizeGripGlyphHasTestTag()

    @Test fun `dragging header resize thumb down grows header height`() =
        GridNavTestScenarios.draggingHeaderResizeThumbDownGrowsHeaderHeight()

    @Test fun `dragging header resize thumb up shrinks header height`() =
        GridNavTestScenarios.draggingHeaderResizeThumbUpShrinksHeaderHeight()

    @Test fun `dragging header resize thumb clamps at max height`() =
        GridNavTestScenarios.draggingHeaderResizeThumbClampsAtMaxHeight()

    @Test fun `renaming app via API preserves dragged header height`() =
        GridNavTestScenarios.renamingAppViaApiPreservesDraggedHeaderHeight()

    @Test fun `header resize thumb still responds after API rename`() =
        GridNavTestScenarios.headerResizeThumbStillRespondsAfterApiRename()

    @Test fun `column resize thumb has test tag`() =
        GridNavTestScenarios.columnResizeThumbHasTestTag()

    @Test fun `column resize grip glyph has test tag`() =
        GridNavTestScenarios.columnResizeGripGlyphHasTestTag()

    @Test fun `dragging column resize thumb right grows resource label column`() =
        GridNavTestScenarios.draggingColumnResizeThumbRightGrowsResourceLabelColumn()

    @Test fun `dragging column resize thumb left shrinks resource label column`() =
        GridNavTestScenarios.draggingColumnResizeThumbLeftShrinksResourceLabelColumn()

    @Test fun `dragging column resize thumb clamps at max width`() =
        GridNavTestScenarios.draggingColumnResizeThumbClampsAtMaxWidth()

    @Test fun `dragging column resize thumb clamps at min width`() =
        GridNavTestScenarios.draggingColumnResizeThumbClampsAtMinWidth()

    @Test fun `selected environment shows as title in grid header`() =
        GridNavTestScenarios.selectedEnvironmentShowsAsTitleInGridHeader()

    @Test fun `grid title updates when environment dropdown changes`() =
        GridNavTestScenarios.gridTitleUpdatesWhenEnvironmentDropdownChanges()

    @Test fun `grid environment heading dropdown changes environment and clears keynav selections`() =
        GridNavTestScenarios.gridEnvironmentHeadingDropdownChangesEnvironmentAndClearsKeyNavSelections()

    @Test fun `grid cells show and hide based on selected environment`() =
        GridNavTestScenarios.gridCellsShowAndHideBasedOnSelectedEnvironment()

    @Test fun `guidance marker appears for cells with guidance`() =
        GridNavTestScenarios.guidanceMarkerAppearsForCellsWithGuidance()

    @Test fun `guidance marker absent for cells without guidance`() =
        GridNavTestScenarios.guidanceMarkerAbsentForCellsWithoutGuidance()

    @Test fun `guidance marker visibility follows environment selection`() =
        GridNavTestScenarios.guidanceMarkerVisibilityFollowsEnvironmentSelection()

    @Test fun `cell click activation - cell click activates pre-prod resource`() =
        GridNavTestScenarios.cellClickActivatesPreProdResource()

    @Test fun `wasm command activator cell is hidden and cannot execute`() =
        GridNavTestScenarios.wasmCommandActivatorCellIsHiddenAndCannotExecute()

    @Test fun `cell click activation - cell click activates prod resource`() =
        GridNavTestScenarios.cellClickActivatesProdResource()

    @Test fun `column header click activates all resources in column`() =
        GridNavTestScenarios.columnHeaderClickActivatesAllResourcesInColumn()

    @Test fun `row label click activates all apps in row`() =
        GridNavTestScenarios.rowLabelClickActivatesAllAppsInRow()

    @Test fun `shift-select activates multiple cells on release`() =
        GridNavTestScenarios.shiftSelectActivatesMultipleCellsOnRelease()

    @Test fun `shift-selected cells visually indicate selection`() =
        GridNavTestScenarios.shiftSelectedCellsVisuallyIndicateSelection()

    @Test fun `keynav dropdowns reset to defaults after grid activation`() =
        GridNavTestScenarios.keyNavDropdownsResetToDefaultsAfterGridActivation()

    @Test fun `all-envs section heading appears above all-envs resources`() =
        GridNavTestScenarios.allEnvsSectionHeadingAppearsAboveAllEnvsResources()

    @Test fun `all-envs section is absent when springboard has no all-envs activators`() =
        GridNavTestScenarios.allEnvsSectionIsAbsentWhenSpringboardHasNoAllEnvsActivators()

    @Test fun `all-envs cell activates all-envs activator regardless of selected environment`() =
        GridNavTestScenarios.allEnvsCellActivatesAllEnvsActivatorRegardlessOfSelectedEnvironment()

    @Test fun `all-envs section renders when no environment is selected`() =
        GridNavTestScenarios.allEnvsSectionRendersWhenNoEnvironmentSelected()

    @Test fun `app groups render columns in group order with separators`() =
        GridNavTestScenarios.appGroupsRenderColumnsInGroupOrderWithSeparators()

    @Test fun `app groups are absent from layout when none declared`() =
        GridNavTestScenarios.appGroupsAreAbsentFromLayoutWhenNoneDeclared()

    @Test fun `hovering row header highlights cells in that row`() =
        GridNavTestScenarios.hoveringRowHeaderHighlightsCellsInThatRow()

    @Test fun `moving pointer from activator cell to row header clears activator preview`() =
        GridNavTestScenarios.movingPointerFromActivatorCellToRowHeaderClearsActivatorPreview()

    @Test fun `clicking column header activates all resources in column`() =
        GridNavTestScenarios.clickingColumnHeaderActivatesAllResourcesInColumn()

    @Test fun `clicking column header in app-groups layout activates all resources in column`() =
        GridNavTestScenarios.clickingColumnHeaderInAppGroupsLayoutActivatesAllResourcesInColumn()

    @Test fun `header resize drag still works with group label strip present`() =
        GridNavTestScenarios.headerResizeDragStillWorksWithGroupLabelStripPresent()
}
