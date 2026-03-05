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

actual fun formatTimestamp(millis: Long): String {
    // Simple timestamp for WASM - just show the millis as a readable date would require JS interop
    return millis.toString()
}

actual fun getPlatformName(): String = "Web"
