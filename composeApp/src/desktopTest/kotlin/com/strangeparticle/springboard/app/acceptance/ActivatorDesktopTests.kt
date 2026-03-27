package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class ActivatorDesktopTests {

    @Ignore @Test fun `command activator runs valid command`() =
        ActivatorDesktopTestScenarios.commandActivatorRunsValidCommand()

    @Ignore @Test fun `command activator shows error for unsuccessful command`() =
        ActivatorDesktopTestScenarios.commandActivatorShowsErrorForUnsuccessfulCommand()

    @Ignore @Test fun `single URL activation opens a new Safari window`() =
        ActivatorDesktopTestScenarios.singleUrlActivationOpensANewSafariWindow()

    @Ignore @Test fun `multi-URL activation opens one new Safari window with all URLs`() =
        ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewSafariWindowWithAllUrls()

    @Ignore @Test fun `single URL activation opens a new Chrome window`() =
        ActivatorDesktopTestScenarios.singleUrlActivationOpensANewChromeWindow()

    @Ignore @Test fun `multi-URL activation opens one new Chrome window with all URLs`() =
        ActivatorDesktopTestScenarios.multiUrlActivationOpensOneNewChromeWindowWithAllUrls()

    @Ignore @Test fun `url activation fallback unsupported browser - single URL falls back to normal opening for unsupported browser`() =
        ActivatorDesktopTestScenarios.singleUrlFallsBackToNormalOpeningForUnsupportedBrowser()

    @Ignore @Test fun `url activation fallback unsupported browser - multi-URL falls back to normal opening for unsupported browser`() =
        ActivatorDesktopTestScenarios.multiUrlFallsBackToNormalOpeningForUnsupportedBrowser()

    @Ignore @Test fun `broken browser integration falls back to normal opening when fallback enabled`() =
        ActivatorDesktopTestScenarios.brokenBrowserIntegrationFallsBackToNormalOpeningWhenFallbackEnabled()
}
