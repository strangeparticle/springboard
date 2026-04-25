package com.strangeparticle.springboard.app.platform

interface PlatformActivationService {
    fun openUrl(url: String)
    fun openUrls(urls: List<String>) { urls.forEach { openUrl(it) } }
    fun executeCommand(command: String, onError: (String) -> Unit)
    fun openNewBrowserWindowIfAppropriate()
    fun hideApplicationViaPid() {}
}
