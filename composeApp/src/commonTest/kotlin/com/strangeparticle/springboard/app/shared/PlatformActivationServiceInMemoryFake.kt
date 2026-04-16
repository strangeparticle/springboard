package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.PlatformActivationService

class PlatformActivationServiceInMemoryFake : PlatformActivationService {
    val openedUrls: MutableList<String> = mutableListOf()
    val executedCommands: MutableList<String> = mutableListOf()
    var newBrowserWindowRequestCount: Int = 0

    override fun openUrl(url: String) {
        openedUrls.add(url)
    }

    override fun executeCommand(command: String, onError: (String) -> Unit) {
        executedCommands.add(command)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        newBrowserWindowRequestCount++
    }
}
