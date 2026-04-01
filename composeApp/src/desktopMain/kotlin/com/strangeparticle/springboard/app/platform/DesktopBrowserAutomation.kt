package com.strangeparticle.springboard.app.platform


// set in main.kt if dev-mode command-line launches declare this param
//   ./gradlew :composeApp:run --args="--surface-applescript-errors"
internal var surfaceAppleScriptErrors = false

actual fun openNewBrowserWindowIfAppropriate(): Boolean {
    return openNewBrowserWindow(
        PlatformBrowserDetectionServiceDefaultImpl(),
        PlatformAppleScriptRunnerServiceDefaultImpl(),
        surfaceAppleScriptErrors,
    )
}

internal fun openNewBrowserWindow(
    browserDetectionService: PlatformBrowserDetectionService,
    appleScriptRunnerService: PlatformAppleScriptRunnerService,
    surfaceErrors: Boolean,
): Boolean {
    val browser = browserDetectionService.detectDefaultBrowser()
    val scriptPath = when (browser) {
        DesktopBrowser.Chrome -> "applescript/chrome/open_new_window.applescript"
        DesktopBrowser.Safari -> "applescript/safari/open_new_window.applescript"
        DesktopBrowser.Unsupported -> return false
    }

    return try {
        val result = appleScriptRunnerService.runAppleScriptFile(scriptPath)
        if (result.exitCode != 0) {
            if (surfaceErrors) {
                throw IllegalStateException("AppleScript failed: ${result.errorSummary()}")
            }
            false
        } else {
            true
        }
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: Exception) {
        if (surfaceErrors) {
            throw IllegalStateException("AppleScript execution failed: ${e.message}", e)
        }
        false
    }
}
