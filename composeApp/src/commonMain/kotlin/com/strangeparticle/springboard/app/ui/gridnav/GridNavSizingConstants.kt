package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants

/**
 * Sizing constants specific to the grid nav header. Kept separate from
 * brand-wide common ui constants because these only matter inside gridnav.
 */
object GridNavSizingConstants {
    val MinHeaderHeight = 60.dp
    val MaxHeaderHeight = 320.dp

    /** Height of the group label strip rendered below rotated column headers. */
    val GroupLabelStripHeight = 14.dp

    val MinResourceLabelWidth = CommonUiConstants.ResourceLabelWidth
    val MaxResourceLabelWidth = 400.dp

    /** Visual thickness of the boundary divider line between header and data rows. */
    val HeaderResizeBoundaryThickness = 2.dp

    /** Visual thickness of the boundary divider line between resource labels and data columns. */
    val ColumnResizeBoundaryThickness = HeaderResizeBoundaryThickness

    /** Vertical hit area of the resize thumb (taller than the divider so it is grabbable). */
    val HeaderResizeThumbHeight = 20.dp

    /** Horizontal width of the centered grip thumb hit area. */
    val HeaderResizeThumbWidth = 28.dp

    /** Horizontal hit area of the column resize thumb (wider than the divider so it is grabbable). */
    val ColumnResizeThumbWidth = HeaderResizeThumbHeight

    /** Vertical height of the centered column resize thumb hit area. */
    val ColumnResizeThumbHeight = HeaderResizeThumbWidth

    /** Width of the Material drag indicator glyph drawn inside the thumb. */
    val HeaderResizeGripWidth = 20.dp

    /** Width of the Material drag indicator glyph drawn inside the column resize thumb. */
    val ColumnResizeGripWidth = HeaderResizeGripWidth

    /** Height of the Material drag indicator glyph drawn inside the thumb. */
    val HeaderResizeGripHeight = 20.dp

    /** Height of the Material drag indicator glyph drawn inside the column resize thumb. */
    val ColumnResizeGripHeight = HeaderResizeGripHeight
}
