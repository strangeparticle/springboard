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

    @Test fun `selected environment shows as title in grid header`() =
        GridNavTestScenarios.selectedEnvironmentShowsAsTitleInGridHeader()

    @Test fun `grid title updates when environment dropdown changes`() =
        GridNavTestScenarios.gridTitleUpdatesWhenEnvironmentDropdownChanges()

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

    @Test fun `cell click activation - cell click activates prod resource`() =
        GridNavTestScenarios.cellClickActivatesProdResource()

    @Test fun `column header click activates all resources in column`() =
        GridNavTestScenarios.columnHeaderClickActivatesAllResourcesInColumn()

    @Test fun `row label click activates all apps in row`() =
        GridNavTestScenarios.rowLabelClickActivatesAllAppsInRow()

    @Test fun `shift-select activates multiple cells on release`() =
        GridNavTestScenarios.shiftSelectActivatesMultipleCellsOnRelease()

    @Test fun `keynav dropdowns reset to defaults after grid activation`() =
        GridNavTestScenarios.keyNavDropdownsResetToDefaultsAfterGridActivation()

    @Test fun `wildcard cells show in all environments`() =
        GridNavTestScenarios.wildcardCellsShowInAllEnvironments()

    @Test fun `wildcard cell activation works across environments`() =
        GridNavTestScenarios.wildcardCellActivationWorksAcrossEnvironments()
}
