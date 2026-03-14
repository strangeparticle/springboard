package com.strangeparticle.springboard.app.platform


// set in main.kt if dev-mode command-line launches declare this param
//   ./gradlew :composeApp:run --args="--surface-applescript-errors"
internal var surfaceAppleScriptErrors = false

actual fun openNewBrowserWindowIfAppropriate(): Boolean {
    val browser = detectDefaultBrowser()
    val openNewWindowScriptPath = when (browser) {
        DesktopBrowser.Chrome -> "applescript/chrome/open_new_window.applescript"
        DesktopBrowser.Safari -> "applescript/safari/open_new_window.applescript"
        DesktopBrowser.Unsupported -> return false
    }

    return try {
        val result = runAppleScriptFile(openNewWindowScriptPath)
        if (result.exitCode != 0) {
            throwForToastIfAppropriate("AppleScript failed: ${result.errorSummary()}")
            false
        } else {
            true
        }
    } catch (e: Exception) {
        throwForToastIfAppropriate("AppleScript execution failed: ${e.message}", e)
        false
    }
}

/**
 * When SURFACE_APPLESCRIPT_ERRORS is true, throws an exception, which gets caught higher up
 * and turns into an error toast, otherwise does nothing
 */
private fun throwForToastIfAppropriate(message: String, cause: Exception? = null) {
    if (surfaceAppleScriptErrors) {
        throw IllegalStateException(message, cause)
    }
}
