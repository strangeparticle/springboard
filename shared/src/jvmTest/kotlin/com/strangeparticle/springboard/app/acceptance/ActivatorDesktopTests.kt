package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class ActivatorDesktopTests {

    @Test fun `command activator runs valid command`() =
        ActivatorDesktopTestScenarios.commandActivatorRunsValidCommand()

    @Test fun `command activator shows error for unsuccessful command`() =
        ActivatorDesktopTestScenarios.commandActivatorShowsErrorForUnsuccessfulCommand()

    @Test fun `single URL activation opens a new Safari window`() =
        ActivatorDesktopTestScenarios.singleUrlActivationOpensANewSafariWindow()

    @Test fun `multi-URL activation opens one new Safari window with all URLs`() =
        ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewSafariWindowWithAllUrls()

    @Test fun `single URL activation opens a new Chrome window`() =
        ActivatorDesktopTestScenarios.singleUrlActivationOpensANewChromeWindow()

    @Test fun `multi-URL activation opens one new Chrome window with all URLs`() =
        ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewChromeWindowWithAllUrls()

    @Test fun `url activation fallback unsupported browser - single URL falls back to normal opening for unsupported browser`() =
        ActivatorDesktopTestScenarios.singleUrlFallsBackToNormalOpeningForUnsupportedBrowser()

    @Test fun `url activation fallback unsupported browser - multi-URL falls back to normal opening for unsupported browser`() =
        ActivatorDesktopTestScenarios.multiUrlFallsBackToNormalOpeningForUnsupportedBrowser()

    @Test fun `broken browser integration falls back to normal opening when fallback enabled`() =
        ActivatorDesktopTestScenarios.brokenBrowserIntegrationFallsBackToNormalOpeningWhenFallbackEnabled()
}
