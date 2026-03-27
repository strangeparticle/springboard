package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class GridNavTests {

    @Ignore @Test fun `selected environment shows as title in grid header`() =
        GridNavTestScenarios.selectedEnvironmentShowsAsTitleInGridHeader()

    @Ignore @Test fun `grid title updates when environment dropdown changes`() =
        GridNavTestScenarios.gridTitleUpdatesWhenEnvironmentDropdownChanges()

    @Ignore @Test fun `grid cells show and hide based on selected environment`() =
        GridNavTestScenarios.gridCellsShowAndHideBasedOnSelectedEnvironment()

    @Ignore @Test fun `cell click activation - cell click activates pre-prod resource`() =
        GridNavTestScenarios.cellClickActivatesPreProdResource()

    @Ignore @Test fun `cell click activation - cell click activates prod resource`() =
        GridNavTestScenarios.cellClickActivatesProdResource()

    @Ignore @Test fun `column header click activates all resources in column`() =
        GridNavTestScenarios.columnHeaderClickActivatesAllResourcesInColumn()

    @Ignore @Test fun `row label click activates all apps in row`() =
        GridNavTestScenarios.rowLabelClickActivatesAllAppsInRow()

    @Ignore @Test fun `shift-select activates multiple cells on release`() =
        GridNavTestScenarios.shiftSelectActivatesMultipleCellsOnRelease()

    @Ignore @Test fun `keynav dropdowns reset to defaults after grid activation`() =
        GridNavTestScenarios.keyNavDropdownsResetToDefaultsAfterGridActivation()
}
