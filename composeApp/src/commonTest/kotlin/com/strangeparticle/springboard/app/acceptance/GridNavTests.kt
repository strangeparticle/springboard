package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class GridNavTests {

    class EnvironmentTitleDisplayed {
        @Ignore @Test fun `selected environment shows as title in grid header`() =
            GridNavTestScenarios.selectedEnvironmentShowsAsTitleInGridHeader()
    }

    class EnvironmentTitleFollowsDropdownChanges {
        @Ignore @Test fun `grid title updates when environment dropdown changes`() =
            GridNavTestScenarios.gridTitleUpdatesWhenEnvironmentDropdownChanges()
    }

    class ActivatorVisibilityByEnvironment {
        @Ignore @Test fun `grid cells show and hide based on selected environment`() =
            GridNavTestScenarios.gridCellsShowAndHideBasedOnSelectedEnvironment()
    }

    class CellClickActivation {
        @Ignore @Test fun `cell click activates pre-prod resource`() =
            GridNavTestScenarios.cellClickActivatesPreProdResource()

        @Ignore @Test fun `cell click activates prod resource`() =
            GridNavTestScenarios.cellClickActivatesProdResource()
    }

    class ColumnClickActivation {
        @Ignore @Test fun `column header click activates all resources in column`() =
            GridNavTestScenarios.columnHeaderClickActivatesAllResourcesInColumn()
    }

    class RowClickActivation {
        @Ignore @Test fun `row label click activates all apps in row`() =
            GridNavTestScenarios.rowLabelClickActivatesAllAppsInRow()
    }

    class ShiftSelectMultiActivation {
        @Ignore @Test fun `shift-select activates multiple cells on release`() =
            GridNavTestScenarios.shiftSelectActivatesMultipleCellsOnRelease()
    }

    class DropdownsResetAfterGridActivation {
        @Ignore @Test fun `keynav dropdowns reset to defaults after grid activation`() =
            GridNavTestScenarios.keyNavDropdownsResetToDefaultsAfterGridActivation()
    }
}
