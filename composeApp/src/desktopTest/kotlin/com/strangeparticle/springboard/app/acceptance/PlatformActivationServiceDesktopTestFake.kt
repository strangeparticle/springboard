package com.strangeparticle.springboard.app.acceptance

import com.strangeparticle.springboard.app.platform.PlatformActivationService
import com.strangeparticle.springboard.app.platform.openNewBrowserWindow

class PlatformActivationServiceDesktopTestFake(
    val browserDetectionService: PlatformBrowserDetectionServiceInMemoryFake = PlatformBrowserDetectionServiceInMemoryFake(),
    val appleScriptRunnerService: PlatformAppleScriptRunnerServiceInMemoryFake = PlatformAppleScriptRunnerServiceInMemoryFake(),
    private val surfaceAppleScriptErrors: Boolean = false,
    var executeCommandException: Exception? = null,
) : PlatformActivationService {

    val openedUrls: MutableList<String> = mutableListOf()
    val executedCommands: MutableList<String> = mutableListOf()

    override fun openUrl(url: String) {
        openedUrls.add(url)
    }

    override fun executeCommand(command: String, onError: (String) -> Unit) {
        executeCommandException?.let { throw it }
        executedCommands.add(command)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        openNewBrowserWindow(browserDetectionService, appleScriptRunnerService, surfaceAppleScriptErrors)
    }
}
