package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun GridNavScrollbarOverlay(
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
    modifier: Modifier,
) {
    // Scrollbar composables are not reliably supported on wasm; the browser
    // provides its own scroll indicators via CSS overflow.
}
