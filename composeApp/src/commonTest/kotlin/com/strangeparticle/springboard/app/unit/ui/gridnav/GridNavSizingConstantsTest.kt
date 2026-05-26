package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.gridnav.GridNavSizingConstants
import com.strangeparticle.springboard.app.ui.gridnav.GroupLabelTextSizeSp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridNavSizingConstantsTest {

    @Test
    fun `resize grip icon uses 20 by 20 dp for visibility`() {
        assertEquals(20f, GridNavSizingConstants.HeaderResizeGripWidth.value)
        assertEquals(20f, GridNavSizingConstants.HeaderResizeGripHeight.value)
    }

    @Test
    fun `resize thumb hit area encloses grip icon`() {
        assertTrue(GridNavSizingConstants.HeaderResizeThumbWidth >= GridNavSizingConstants.HeaderResizeGripWidth)
        assertTrue(GridNavSizingConstants.HeaderResizeThumbHeight >= GridNavSizingConstants.HeaderResizeGripHeight)
    }

    @Test
    fun `resource label resize bounds contain default width`() {
        assertTrue(GridNavSizingConstants.MinResourceLabelWidth <= CommonUiConstants.ResourceLabelWidth)
        assertTrue(GridNavSizingConstants.MaxResourceLabelWidth > CommonUiConstants.ResourceLabelWidth)
    }

    @Test
    fun `column resize thumb hit area encloses grip icon`() {
        assertTrue(GridNavSizingConstants.ColumnResizeThumbWidth >= GridNavSizingConstants.ColumnResizeGripWidth)
        assertTrue(GridNavSizingConstants.ColumnResizeThumbHeight >= GridNavSizingConstants.ColumnResizeGripHeight)
    }

    @Test
    fun `column boundary divider is thinner than header boundary while resize handle stays unchanged`() {
        assertEquals(2f, GridNavSizingConstants.HeaderResizeBoundaryThickness.value)
        assertEquals(1f, GridNavSizingConstants.ColumnResizeBoundaryThickness.value)
        assertTrue(
            GridNavSizingConstants.ColumnResizeBoundaryThickness <
                GridNavSizingConstants.HeaderResizeBoundaryThickness,
        )
        assertEquals(20f, GridNavSizingConstants.ColumnResizeThumbWidth.value)
        assertEquals(28f, GridNavSizingConstants.ColumnResizeThumbHeight.value)
        assertEquals(20f, GridNavSizingConstants.ColumnResizeGripWidth.value)
        assertEquals(20f, GridNavSizingConstants.ColumnResizeGripHeight.value)
    }

    @Test
    fun `data row divider thickness is available for boundary extension calculations`() {
        assertEquals(0.5f, GridNavSizingConstants.DataRowDividerThickness.value)
    }

    @Test
    fun `group label strip height is tall enough for label text`() {
        assertTrue(
            GridNavSizingConstants.GroupLabelStripHeight.value >= GroupLabelTextSizeSp,
            "GroupLabelStripHeight must be at least as tall as the label text size",
        )
    }
}
