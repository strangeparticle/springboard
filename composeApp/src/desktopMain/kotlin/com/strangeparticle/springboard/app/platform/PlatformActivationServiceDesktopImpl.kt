package com.strangeparticle.springboard.app.platform

class PlatformActivationServiceDesktopImpl(
    private val browserDetectionService: PlatformBrowserDetectionService = PlatformBrowserDetectionServiceDefaultImpl(),
    private val appleScriptRunnerService: PlatformAppleScriptRunnerService = PlatformAppleScriptRunnerServiceDefaultImpl(),
    private val terminalDetectionService: PlatformTerminalDetectionService = PlatformTerminalDetectionServiceDefaultImpl(),
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

    override fun openTerminal(
        workingDirectory: String,
        command: String?,
        preferredTerminal: PreferredTerminal,
        openInNewWindow: Boolean,
        onError: (String) -> Unit,
    ) {
        val resolvedTerminal = resolveTerminalWithFallback(preferredTerminal, onError)
        val commandLine = buildTerminalActivatorCommandLine(workingDirectory, command)
        val scriptPath = terminalAppleScriptResource(resolvedTerminal, openInNewWindow)
        try {
            val result = appleScriptRunnerService.runAppleScriptFile(scriptPath, listOf(commandLine))
            if (result.exitCode != 0 && surfaceAppleScriptErrors) {
                onError("Failed to open terminal: ${result.errorSummary()}")
            }
        } catch (e: Exception) {
            if (surfaceAppleScriptErrors) {
                onError("Failed to open terminal: ${e.message}")
            }
        }
    }

    // iTerm is optional; if the user picked it but it isn't installed, open Terminal
    // instead and tell the user. Terminal.app is always available.
    private fun resolveTerminalWithFallback(
        preferredTerminal: PreferredTerminal,
        onError: (String) -> Unit,
    ): PreferredTerminal {
        val isAvailable = terminalDetectionService.isInstalled(preferredTerminal)
        if (!isAvailable && preferredTerminal == PreferredTerminal.ITerm) {
            onError("iTerm isn't installed — opening Terminal instead.")
            return PreferredTerminal.TerminalApp
        }
        return preferredTerminal
    }

    private fun terminalAppleScriptResource(
        terminal: PreferredTerminal,
        openInNewWindow: Boolean,
    ): String {
        val scriptName = when (terminal) {
            PreferredTerminal.TerminalApp ->
                if (openInNewWindow) "terminal_new_window" else "terminal_front_window"
            PreferredTerminal.ITerm ->
                if (openInNewWindow) "iterm_new_window" else "iterm_front_window_new_tab"
        }
        return "applescript/$scriptName.applescript"
    }
}
