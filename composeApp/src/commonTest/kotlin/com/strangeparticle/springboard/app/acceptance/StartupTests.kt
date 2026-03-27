package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class StartupTests {

    @Test fun `springboard icon is shown in upper left`() =
        StartupTestScenarios.springboardIconIsShownInUpperLeft()

    @Test fun `version identifier displays correct version`() =
        StartupTestScenarios.versionIdentifierDisplaysCorrectVersion()

    @Test fun `springboard with command activators shows security warning`() =
        StartupTestScenarios.springboardWithCommandActivatorsShowsSecurityWarning()

    @Test fun `springboard without command activators skips security warning`() =
        StartupTestScenarios.springboardWithoutCommandActivatorsSkipsSecurityWarning()

    @Test fun `status line shows embedded path for built-in springboard`() =
        StartupTestScenarios.statusLineShowsEmbeddedPathForBuiltInSpringboard()

    @Test fun `status line shows file path for custom-loaded springboard`() =
        StartupTestScenarios.statusLineShowsFilePathForCustomLoadedSpringboard()
}
