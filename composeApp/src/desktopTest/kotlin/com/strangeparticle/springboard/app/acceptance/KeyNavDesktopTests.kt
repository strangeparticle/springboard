package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class KeyNavDesktopTests {

    class MacEnterKeyActivation {
        @Ignore @Test fun `enter key triggers activation on Mac`() =
            KeyNavDesktopTestScenarios.enterKeyTriggersActivationOnMac()
    }

    class LinuxCtrlEnterActivation {
        @Ignore @Test fun `ctrl-enter triggers activation on Linux`() =
            KeyNavDesktopTestScenarios.ctrlEnterTriggersActivationOnLinux()
    }
}
