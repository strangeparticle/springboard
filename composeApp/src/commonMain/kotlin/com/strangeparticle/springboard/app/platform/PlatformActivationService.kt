package com.strangeparticle.springboard.app.platform

interface PlatformActivationService {
    fun openUrl(url: String)
    fun executeCommand(command: String)
    fun openNewBrowserWindowIfAppropriate()
}
