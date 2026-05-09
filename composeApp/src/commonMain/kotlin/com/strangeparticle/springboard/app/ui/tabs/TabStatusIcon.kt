package com.strangeparticle.springboard.app.ui.tabs

import com.strangeparticle.springboard.app.viewmodel.TabState

/**
 * Status indicator overlaid on a tab title in the upper-right corner.
 *
 * The two states are mutually exclusive: [Dirty] takes priority over [NonSaveable]
 * when both would apply (a dirty network-sourced tab). Empty tabs and clean
 * local-file tabs have no status icon.
 */
enum class TabStatusIcon {
    /** The tab has unsaved in-memory edits relative to its source. */
    Dirty,

    /** The tab's source cannot be saved to in place (HTTP / `s3://` URLs). */
    NonSaveable,
}

/**
 * Returns the status icon (if any) for the given tab. Rules per spec §2.3:
 * - dirty (any source) → [TabStatusIcon.Dirty]
 * - clean + non-saveable source (HTTP / S3) → [TabStatusIcon.NonSaveable]
 * - clean local-file tab or empty tab (`source == null`) → null
 */
fun tabStatusIconFor(tab: TabState): TabStatusIcon? {
    if (tab.springboard == null) return null
    if (tab.isDirty) return TabStatusIcon.Dirty
    val source = tab.source ?: return null
    if (isNonSaveableInPlaceSource(source)) return TabStatusIcon.NonSaveable
    return null
}

private fun isNonSaveableInPlaceSource(source: String): Boolean {
    val lowered = source.lowercase()
    return lowered.startsWith("http://") ||
        lowered.startsWith("https://") ||
        lowered.startsWith("s3://")
}
