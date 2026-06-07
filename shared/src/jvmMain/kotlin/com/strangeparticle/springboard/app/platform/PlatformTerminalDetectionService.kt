package com.strangeparticle.springboard.app.platform

interface PlatformTerminalDetectionService {
    fun isInstalled(terminal: PreferredTerminal): Boolean
}
