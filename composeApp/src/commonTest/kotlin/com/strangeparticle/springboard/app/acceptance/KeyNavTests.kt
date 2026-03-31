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

    @Test fun `environment defaults to all - environment defaults to all when present`() =
        KeyNavTestScenarios.environmentDefaultsToAllWhenPresent()

    @Test fun `environment defaults to all - environment defaults to first when all is not present`() =
        KeyNavTestScenarios.environmentDefaultsToFirstWhenAllIsNotPresent()

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
}

// Window refocus test (AppDropdownFocusAfterWindowRefocus) is manual-only:
// Window focus management lives in platform-specific main.kt (desktop and WASM),
// outside the compose tree that CMP UI tests can control.
