package com.strangeparticle.springboard.app.platform

import kotlinx.browser.window

actual fun openUrl(url: String) {
    window.open(url)
}

actual fun openFileDialog(currentPath: String?): String? {
    return null
}

actual fun readFileContents(path: String): String? {
    return null
}

actual fun getPlatformName(): String = "Web"

actual fun openNewBrowserWindowIfAppropriate(): Boolean = false

actual fun saveLocalCopyAsFileDialog(suggestedName: String): String? = null

actual fun writeFileContents(path: String, contents: String): Boolean = false
