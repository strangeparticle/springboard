package com.strangeparticle.springboard.app.platform

interface PlatformActivationService {
    fun openUrl(url: String)
    fun openUrls(urls: List<String>) { urls.forEach { openUrl(it) } }
    fun executeCommand(command: String, onError: (String) -> Unit)
    fun openNewBrowserWindowIfAppropriate()
    fun hideApplicationViaPid() {}

    /**
     * Opens [preferredTerminal] at [workingDirectory], optionally running [command].
     * When [openInNewWindow] is true a dedicated new window is forced; otherwise the
     * terminal places the session per its own preferences. Default no-op so only the
     * desktop target needs to implement it.
     */
    fun openTerminal(
        workingDirectory: String,
        command: String?,
        preferredTerminal: PreferredTerminal,
        openInNewWindow: Boolean,
        onError: (String) -> Unit,
    ) {}
}
