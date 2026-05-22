package com.strangeparticle.springboard.app.ui.tabs

import com.strangeparticle.springboard.app.viewmodel.TabState

/**
 * Status indicators shown next to a tab title.
 *
 * Empty tabs and clean local-file tabs have no status icons. Dirty network-sourced
 * tabs show both [NonSaveable] and [Dirty].
 */
enum class TabStatusIcon {
    /** The tab has unsaved in-memory edits relative to its source. */
    Dirty,

    /** The tab's source cannot be saved to in place (HTTP/HTTPS URLs). */
    NonSaveable,
}

/**
 * Returns the status icons for the given tab. Rules per spec §2.3:
 * - clean + non-saveable source (HTTP/HTTPS) → [TabStatusIcon.NonSaveable]
 * - dirty + non-saveable source (HTTP/HTTPS) → [TabStatusIcon.NonSaveable], [TabStatusIcon.Dirty]
 * - dirty local-file or source-less tab → [TabStatusIcon.Dirty]
 * - clean local-file tab or empty tab (`source == null`) → null
 */
fun tabStatusIconsFor(tab: TabState): List<TabStatusIcon> {
    if (tab.springboard == null) return emptyList()
    val icons = mutableListOf<TabStatusIcon>()
    val source = tab.source
    if (source != null && isNonSaveableInPlaceSource(source)) icons += TabStatusIcon.NonSaveable
    if (tab.isDirty) icons += TabStatusIcon.Dirty
    return icons
}

private fun isNonSaveableInPlaceSource(source: String): Boolean {
    val lowered = source.lowercase()
    return lowered.startsWith("http://") ||
        lowered.startsWith("https://")
}
