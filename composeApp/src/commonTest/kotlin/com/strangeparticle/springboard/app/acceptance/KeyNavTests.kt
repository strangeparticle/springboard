package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class KeyNavTests {

    class AppDropdownFocusOnStartup {
        @Ignore @Test fun `app dropdown gets focus on startup`() =
            KeyNavTestScenarios.appDropdownGetsFocusOnStartup()
    }

    class AppDropdownFocusAfterWindowRefocus {
        @Ignore @Test fun `app dropdown gets focus after switching away and back`() =
            KeyNavTestScenarios.appDropdownGetsFocusAfterSwitchingAwayAndBack()
    }

    class AppDropdownFocusAfterAutoClosedToast {
        @Ignore @Test fun `app dropdown gets focus after toast auto-closes`() =
            KeyNavTestScenarios.appDropdownGetsFocusAfterToastAutoCloses()
    }

    class AppDropdownFocusAfterManuallyClosedToast {
        @Ignore @Test fun `app dropdown gets focus after toast is manually closed`() =
            KeyNavTestScenarios.appDropdownGetsFocusAfterToastIsManuallyClosed()
    }

    class DropdownsResetAfterActivation {
        @Ignore @Test fun `dropdowns reset to defaults after keynav activation`() =
            KeyNavTestScenarios.dropdownsResetToDefaultsAfterKeyNavActivation()
    }

    class EnvironmentDefaultsToAll {
        @Ignore @Test fun `environment defaults to all when present`() =
            KeyNavTestScenarios.environmentDefaultsToAllWhenPresent()

        @Ignore @Test fun `environment defaults to first when all is not present`() =
            KeyNavTestScenarios.environmentDefaultsToFirstWhenAllIsNotPresent()
    }

    class ResourceOptionsDisabledBasedOnAppChoice {
        @Ignore @Test fun `unavailable resources are disabled for selected app`() =
            KeyNavTestScenarios.unavailableResourcesAreDisabledForSelectedApp()
    }

    class ResourceResetWhenUnavailableAfterAppChange {
        @Ignore @Test fun `selected resource resets when unavailable after app change`() =
            KeyNavTestScenarios.selectedResourceResetsWhenUnavailableAfterAppChange()
    }

    class ResourceRetainedWhenAvailableAfterAppChange {
        @Ignore @Test fun `selected resource is retained when available after app change`() =
            KeyNavTestScenarios.selectedResourceIsRetainedWhenAvailableAfterAppChange()
    }

    class KeyNavActivationPreProd {
        @Ignore @Test fun `keynav activation works for pre-prod environment`() =
            KeyNavTestScenarios.keyNavActivationWorksForPreProdEnvironment()
    }

    class KeyNavActivationProd {
        @Ignore @Test fun `keynav activation works for prod environment`() =
            KeyNavTestScenarios.keyNavActivationWorksForProdEnvironment()
    }
}
