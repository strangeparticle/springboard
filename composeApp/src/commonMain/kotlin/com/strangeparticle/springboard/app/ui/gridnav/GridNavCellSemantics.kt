package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Custom semantic property that exposes a grid cell's "row highlighted" state
 * (true when its row is highlighted because the pointer is over a data cell in
 * that row, or over the row's header label). Used exclusively by UI tests to
 * verify the row-hover crosshair propagates from the row header into the row's
 * data cells. Production code does not rely on it.
 */
val IsRowHighlightedKey = SemanticsPropertyKey<Boolean>("IsRowHighlighted")
var SemanticsPropertyReceiver.isRowHighlighted by IsRowHighlightedKey
