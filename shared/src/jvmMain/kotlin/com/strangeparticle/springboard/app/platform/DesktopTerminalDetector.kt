package com.strangeparticle.springboard.app.platform

private enum class TerminalBundleId(val value: String) {
    ITerm("com.googlecode.iterm2"),
}

// Terminal.app ships with macOS, so it is always considered installed. iTerm is
// optional: we ask Spotlight's Launch Services index whether a bundle with iTerm's
// identifier exists. This pre-flight check lets the activator fall back cleanly
// instead of triggering the "Where is iTerm?" chooser when iTerm is absent.
internal fun isTerminalInstalled(terminal: PreferredTerminal): Boolean = when (terminal) {
    PreferredTerminal.TerminalApp -> true
    PreferredTerminal.ITerm -> isAppInstalledByBundleId(TerminalBundleId.ITerm.value)
}

private fun isAppInstalledByBundleId(bundleId: String): Boolean {
    val result = try {
        runShellCommand("/usr/bin/mdfind", "kMDItemCFBundleIdentifier == '$bundleId'")
    } catch (e: Exception) {
        return false
    }
    return result.exitCode == 0 && result.stdout.isNotBlank()
}
