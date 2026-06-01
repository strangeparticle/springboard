package com.strangeparticle.springboard.app.platform

class PlatformTerminalDetectionServiceDefaultImpl : PlatformTerminalDetectionService {
    override fun isInstalled(terminal: PreferredTerminal): Boolean =
        com.strangeparticle.springboard.app.platform.isTerminalInstalled(terminal)
}
