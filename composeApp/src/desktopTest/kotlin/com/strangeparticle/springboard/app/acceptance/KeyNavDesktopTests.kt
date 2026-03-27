package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class KeyNavDesktopTests {

    @Ignore @Test fun `enter key triggers activation on Mac`() =
        KeyNavDesktopTestScenarios.enterKeyTriggersActivationOnMac()

    @Ignore @Test fun `ctrl-enter triggers activation on Linux`() =
        KeyNavDesktopTestScenarios.ctrlEnterTriggersActivationOnLinux()
}
