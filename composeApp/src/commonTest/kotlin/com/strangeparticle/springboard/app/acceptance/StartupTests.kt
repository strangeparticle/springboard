package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class StartupTests {

    class AppIconVisible {
        @Ignore @Test fun `springboard icon is shown in upper left`() =
            StartupTestScenarios.springboardIconIsShownInUpperLeft()
    }

    class VersionIdentifierCorrect {
        @Ignore @Test fun `version identifier displays correct version`() =
            StartupTestScenarios.versionIdentifierDisplaysCorrectVersion()
    }

    class CommandLineActivatorWarningShown {
        @Ignore @Test fun `springboard with command activators shows security warning`() =
            StartupTestScenarios.springboardWithCommandActivatorsShowsSecurityWarning()
    }

    class CommandLineActivatorWarningSkippedWhenNoCommandActivators {
        @Ignore @Test fun `springboard without command activators skips security warning`() =
            StartupTestScenarios.springboardWithoutCommandActivatorsSkipsSecurityWarning()
    }

    class StatusLineShowsEmbeddedPath {
        @Ignore @Test fun `status line shows embedded path for built-in springboard`() =
            StartupTestScenarios.statusLineShowsEmbeddedPathForBuiltInSpringboard()
    }

    class StatusLineShowsCustomLoadedPath {
        @Ignore @Test fun `status line shows file path for custom-loaded springboard`() =
            StartupTestScenarios.statusLineShowsFilePathForCustomLoadedSpringboard()
    }
}
