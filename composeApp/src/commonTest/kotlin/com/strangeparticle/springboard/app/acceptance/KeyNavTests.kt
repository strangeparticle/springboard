package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class KeyNavTests {

    @Test fun `app dropdown gets focus on startup`() =
        KeyNavTestScenarios.appDropdownGetsFocusOnStartup()

    @Test fun `app dropdown gets focus after toast auto-closes`() =
        KeyNavTestScenarios.appDropdownGetsFocusAfterToastAutoCloses()

    @Test fun `app dropdown gets focus after toast is manually closed`() =
        KeyNavTestScenarios.appDropdownGetsFocusAfterToastIsManuallyClosed()

    @Test fun `dropdowns reset to defaults after keynav activation`() =
        KeyNavTestScenarios.dropdownsResetToDefaultsAfterKeyNavActivation()

    @Test fun `environment defaults to first declared environment`() =
        KeyNavTestScenarios.environmentDefaultsToFirstDeclaredEnvironment()

    @Test fun `environment defaults to first declared environment in two-env fixture`() =
        KeyNavTestScenarios.environmentDefaultsToFirstDeclaredEnvironmentInTwoEnvFixture()

    @Test fun `unavailable resources are disabled for selected app`() =
        KeyNavTestScenarios.unavailableResourcesAreDisabledForSelectedApp()

    @Test fun `selected resource resets when unavailable after app change`() =
        KeyNavTestScenarios.selectedResourceResetsWhenUnavailableAfterAppChange()

    @Test fun `selected resource is retained when available after app change`() =
        KeyNavTestScenarios.selectedResourceIsRetainedWhenAvailableAfterAppChange()

    @Test fun `keynav activation works for pre-prod environment`() =
        KeyNavTestScenarios.keyNavActivationWorksForPreProdEnvironment()

    @Test fun `keynav activation works for prod environment`() =
        KeyNavTestScenarios.keyNavActivationWorksForProdEnvironment()

    @Test fun `all-envs activator is activatable when only app and resource selected`() =
        KeyNavTestScenarios.allEnvsActivatorIsActivatableWhenOnlyAppAndResourceSelected()

    @Test fun `all-envs activator is not confused with env-specific activator in dropdown states`() =
        KeyNavTestScenarios.allEnvsActivatorIsNotConfusedWithEnvSpecificActivatorInDropdownStates()

    @Test fun `changing environment keeps app and resource when still valid`() =
        KeyNavTestScenarios.changingEnvironmentKeepsAppAndResourceWhenStillValid()

    @Test fun `environment options are filtered by selected app and resource`() =
        KeyNavTestScenarios.environmentOptionsAreFilteredBySelectedAppAndResource()

    @Test fun `dropdowns include none option to clear selection`() =
        KeyNavTestScenarios.dropdownsIncludeNoneOptionToClearSelection()

    @Test fun `environment dropdown includes none option`() =
        KeyNavTestScenarios.environmentDropdownIncludesNoneOption()

    @Test fun `environment dropdown none clears only environment selection`() =
        KeyNavTestScenarios.environmentDropdownNoneClearsOnlyEnvironmentSelection()

    @Test fun `grid is hidden when environment selection is none`() =
        KeyNavTestScenarios.gridIsHiddenWhenEnvironmentSelectionIsNone()

    @Test fun `typing selects entry when dropdown is open`() =
        KeyNavTestScenarios.typingSelectsEntryWhenDropdownIsOpen()

    @Test fun `shift-tab moves backward and wraps across dropdown series`() =
        KeyNavTestScenarios.shiftTabMovesBackwardAndWrapsAcrossDropdownSeries()

    @Test fun `shift-tab does not activate from environment dropdown`() =
        KeyNavTestScenarios.shiftTabDoesNotActivateFromEnvironmentDropdown()

    @Test fun `escape clears all selections and focuses app dropdown`() =
        KeyNavTestScenarios.escapeClearsAllSelectionsAndFocusesAppDropdown()

}

// Window refocus test (AppDropdownFocusAfterWindowRefocus) is manual-only:
// Window focus management lives in platform-specific main.kt (desktop and WASM),
// outside the compose tree that CMP UI tests can control.
