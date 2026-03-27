package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class KeyNavTests {

    @Ignore @Test fun `app dropdown gets focus on startup`() =
        KeyNavTestScenarios.appDropdownGetsFocusOnStartup()

    @Ignore @Test fun `app dropdown gets focus after switching away and back`() =
        KeyNavTestScenarios.appDropdownGetsFocusAfterSwitchingAwayAndBack()

    @Ignore @Test fun `app dropdown gets focus after toast auto-closes`() =
        KeyNavTestScenarios.appDropdownGetsFocusAfterToastAutoCloses()

    @Ignore @Test fun `app dropdown gets focus after toast is manually closed`() =
        KeyNavTestScenarios.appDropdownGetsFocusAfterToastIsManuallyClosed()

    @Ignore @Test fun `dropdowns reset to defaults after keynav activation`() =
        KeyNavTestScenarios.dropdownsResetToDefaultsAfterKeyNavActivation()

    @Ignore @Test fun `environment defaults to all - environment defaults to all when present`() =
        KeyNavTestScenarios.environmentDefaultsToAllWhenPresent()

    @Ignore @Test fun `environment defaults to all - environment defaults to first when all is not present`() =
        KeyNavTestScenarios.environmentDefaultsToFirstWhenAllIsNotPresent()

    @Ignore @Test fun `unavailable resources are disabled for selected app`() =
        KeyNavTestScenarios.unavailableResourcesAreDisabledForSelectedApp()

    @Ignore @Test fun `selected resource resets when unavailable after app change`() =
        KeyNavTestScenarios.selectedResourceResetsWhenUnavailableAfterAppChange()

    @Ignore @Test fun `selected resource is retained when available after app change`() =
        KeyNavTestScenarios.selectedResourceIsRetainedWhenAvailableAfterAppChange()

    @Ignore @Test fun `keynav activation works for pre-prod environment`() =
        KeyNavTestScenarios.keyNavActivationWorksForPreProdEnvironment()

    @Ignore @Test fun `keynav activation works for prod environment`() =
        KeyNavTestScenarios.keyNavActivationWorksForProdEnvironment()
}
