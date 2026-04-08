package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.unit.dp

/**
 * Sizing constants specific to the grid nav header. Kept separate from
 * brand-wide common ui constants because these only matter inside gridnav.
 */
object GridNavSizingConstants {
    val MinHeaderHeight = 60.dp
    val MaxHeaderHeight = 320.dp

    /** Visual thickness of the boundary divider line between header and data rows. */
    val HeaderResizeBoundaryThickness = 2.dp

    /** Vertical hit area of the resize thumb (taller than the divider so it is grabbable). */
    val HeaderResizeThumbHeight = 20.dp

    /** Horizontal width of the centered grip thumb hit area. */
    val HeaderResizeThumbWidth = 28.dp

    /** Width of the Material drag indicator glyph drawn inside the thumb. */
    val HeaderResizeGripWidth = 20.dp

    /** Height of the Material drag indicator glyph drawn inside the thumb. */
    val HeaderResizeGripHeight = 20.dp
}
