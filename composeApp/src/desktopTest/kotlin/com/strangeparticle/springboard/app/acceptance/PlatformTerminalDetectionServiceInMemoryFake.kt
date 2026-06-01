package com.strangeparticle.springboard.app.acceptance

import com.strangeparticle.springboard.app.platform.PlatformTerminalDetectionService
import com.strangeparticle.springboard.app.platform.PreferredTerminal

class PlatformTerminalDetectionServiceInMemoryFake(
    var iTermInstalled: Boolean = true,
) : PlatformTerminalDetectionService {
    override fun isInstalled(terminal: PreferredTerminal): Boolean = when (terminal) {
        PreferredTerminal.TerminalApp -> true
        PreferredTerminal.ITerm -> iTermInstalled
    }
}
