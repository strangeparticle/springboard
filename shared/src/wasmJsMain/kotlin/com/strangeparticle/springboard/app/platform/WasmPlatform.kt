package com.strangeparticle.springboard.app.platform

import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import kotlinx.browser.window

actual fun openUrl(url: String) {
    window.open(url)
}

private var multiplePopupsAllowed = false

actual fun openUrls(urls: List<String>) {
    if (urls.isEmpty()) return
    if (urls.size == 1) {
        window.open(urls.first(), "_blank")
        return
    }
    if (!multiplePopupsAllowed) {
        // Probe whether the browser allows multiple popups by opening a blank
        // window without navigating away. If blocked, the browser shows its
        // popup-blocked icon on this tab (which still has focus).
        val probe = window.open("", "_blank")
        if (probe == null) {
            ToastBroadcaster.info(
                "Allow popups for this site to open multiple links at once, then retry."
            )
            return
        }
        probe.close()
        multiplePopupsAllowed = true
    }
    for (url in urls) {
        window.open(url, "_blank")
    }
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
