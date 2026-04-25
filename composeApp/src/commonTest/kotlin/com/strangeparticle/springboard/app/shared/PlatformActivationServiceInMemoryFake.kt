package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.PlatformActivationService

class PlatformActivationServiceInMemoryFake : PlatformActivationService {
    val openedUrls: MutableList<String> = mutableListOf()
    val executedCommands: MutableList<String> = mutableListOf()
    var newBrowserWindowRequestCount: Int = 0
    var hideApplicationViaPidCount: Int = 0
    var openUrlsException: Exception? = null

    override fun openUrl(url: String) {
        openedUrls.add(url)
    }

    override fun openUrls(urls: List<String>) {
        openUrlsException?.let { throw it }
        urls.forEach { openedUrls.add(it) }
    }

    override fun executeCommand(command: String, onError: (String) -> Unit) {
        executedCommands.add(command)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        newBrowserWindowRequestCount++
    }

    override fun hideApplicationViaPid() {
        hideApplicationViaPidCount++
    }
}
