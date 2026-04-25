package com.strangeparticle.springboard.app.platform

class PlatformActivationServiceDesktopImpl(
    private val browserDetectionService: PlatformBrowserDetectionService = PlatformBrowserDetectionServiceDefaultImpl(),
    private val appleScriptRunnerService: PlatformAppleScriptRunnerService = PlatformAppleScriptRunnerServiceDefaultImpl(),
    private val surfaceAppleScriptErrors: Boolean = false,
) : PlatformActivationService {

    override fun openUrl(url: String) {
        com.strangeparticle.springboard.app.platform.openUrl(url)
    }

    override fun executeCommand(command: String, onError: (String) -> Unit) {
        com.strangeparticle.springboard.app.platform.executeCommand(command, onError)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        openNewBrowserWindow(browserDetectionService, appleScriptRunnerService, surfaceAppleScriptErrors)
    }

    override fun hideApplicationViaPid() {
        val pid = ProcessHandle.current().pid().toString()
        try {
            appleScriptRunnerService.runAppleScriptFile(
                "applescript/hide_application_via_pid.applescript",
                listOf(pid),
            )
        } catch (e: Exception) {
            println("[Springboard] hideApplicationViaPid failed: ${e.message}")
        }
    }
}
