package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class ActivatorDesktopTests {

    class CommandActivation {
        @Ignore @Test fun `command activator runs valid command`() =
            ActivatorDesktopTestScenarios.commandActivatorRunsValidCommand()
    }

    class CommandActivationErrorHandling {
        @Ignore @Test fun `command activator shows error for unsuccessful command`() =
            ActivatorDesktopTestScenarios.commandActivatorShowsErrorForUnsuccessfulCommand()
    }

    class UrlActivationNewWindowSafari {
        @Ignore @Test fun `single URL activation opens a new Safari window`() =
            ActivatorDesktopTestScenarios.singleUrlActivationOpensANewSafariWindow()
    }

    class UrlActivationNewWindowSafariMultiple {
        @Ignore @Test fun `multi-URL activation opens one new Safari window with all URLs`() =
            ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewSafariWindowWithAllUrls()
    }

    class UrlActivationNewWindowChrome {
        @Ignore @Test fun `single URL activation opens a new Chrome window`() =
            ActivatorDesktopTestScenarios.singleUrlActivationOpensANewChromeWindow()
    }

    class UrlActivationNewWindowChromeMultiple {
        @Ignore @Test fun `multi-URL activation opens one new Chrome window with all URLs`() =
            ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewChromeWindowWithAllUrls()
    }

    class UrlActivationFallbackUnsupportedBrowser {
        @Ignore @Test fun `single URL falls back to normal opening for unsupported browser`() =
            ActivatorDesktopTestScenarios.singleUrlFallsBackToNormalOpeningForUnsupportedBrowser()

        @Ignore @Test fun `multi-URL falls back to normal opening for unsupported browser`() =
            ActivatorDesktopTestScenarios.multiUrlFallsBackToNormalOpeningForUnsupportedBrowser()
    }

    class UrlActivationFallbackBrokenIntegration {
        @Ignore @Test fun `broken browser integration falls back to normal opening when fallback enabled`() =
            ActivatorDesktopTestScenarios.brokenBrowserIntegrationFallsBackToNormalOpeningWhenFallbackEnabled()
    }
}
