package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.gridnav.GridNavSizingConstants
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
}
