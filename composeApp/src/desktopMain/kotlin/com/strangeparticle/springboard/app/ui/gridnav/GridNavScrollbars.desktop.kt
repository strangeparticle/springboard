package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun GridNavScrollbarOverlay(
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        VerticalScrollbar(
            adapter = ScrollbarAdapter(verticalScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
        HorizontalScrollbar(
            adapter = ScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
}
