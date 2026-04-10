package com.strangeparticle.springboard.app.platform

class PlatformActivationServiceDefaultImpl : PlatformActivationService {

    override fun openUrl(url: String) {
        com.strangeparticle.springboard.app.platform.openUrl(url)
    }

    override fun openUrls(urls: List<String>) {
        com.strangeparticle.springboard.app.platform.openUrls(urls)
    }

    override fun executeCommand(command: String) {
        com.strangeparticle.springboard.app.platform.executeCommand(command)
    }

    override fun openNewBrowserWindowIfAppropriate() {
        com.strangeparticle.springboard.app.platform.openNewBrowserWindowIfAppropriate()
    }
}
