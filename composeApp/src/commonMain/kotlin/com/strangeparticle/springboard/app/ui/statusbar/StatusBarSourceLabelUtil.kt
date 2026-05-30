package com.strangeparticle.springboard.app.ui.statusbar

import com.strangeparticle.springboard.app.platform.formatTimestamp

/**
 * Builds the text shown in the status bar source area.
 *
 * An unsaved/new springboard has no backing file or URL, so its `source` is
 * blank. In that case show a clear `<unsaved>` placeholder instead of an empty
 * path followed by a load timestamp.
 */
fun statusBarSourceLabel(source: String, lastLoadTime: Long): String {
    if (source.isBlank()) {
        return "<unsaved>"
    }
    return "$source @ ${formatTimestamp(lastLoadTime)}"
}
