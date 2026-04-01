package com.strangeparticle.springboard.app.platform

class PlatformActivationServiceDesktopImpl(
    private val browserDetectionService: PlatformBrowserDetectionService = PlatformBrowserDetectionServiceDefaultImpl(),
    private val appleScriptRunnerService: PlatformAppleScriptRunnerService = PlatformAppleScriptRunnerServiceDefaultImpl(),
    private val surfaceAppleScriptErrors: Boolean = false,
) : PlatformActivationService {

    override fun openUrl(url: String) {
        com.strangeparticle.springboard.app.platform.openUrl(url)
    }

    override fun executeCommand(command: String) {
        com.strangeparticle.springboard.app.platform.executeCommand(command)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        openNewBrowserWindow(browserDetectionService, appleScriptRunnerService, surfaceAppleScriptErrors)
    }
}
