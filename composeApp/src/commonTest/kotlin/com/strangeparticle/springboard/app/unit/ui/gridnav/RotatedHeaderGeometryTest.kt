package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.gridnav.Sin45
import com.strangeparticle.springboard.app.ui.gridnav.computeRotatedHeaderHeightPx
import kotlin.test.Test
import kotlin.test.assertEquals

class RotatedHeaderGeometryTest {

    @Test
    fun computeRotatedHeaderHeightPx_returnsWidthPlusHeightTimesSin45() {
        val textWidth = 100f
        val stackedHeight = 27f
        val expected = (textWidth + stackedHeight) * Sin45
        assertEquals(expected, computeRotatedHeaderHeightPx(textWidth, stackedHeight))
    }

    @Test
    fun computeRotatedHeaderHeightPx_withZeroTextWidth_returnsStackedHeightTimesSin45() {
        val result = computeRotatedHeaderHeightPx(0f, 27f)
        assertEquals(27f * Sin45, result)
    }

    @Test
    fun computeRotatedHeaderHeightPx_withZeroStackedHeight_returnsTextWidthTimesSin45() {
        val result = computeRotatedHeaderHeightPx(100f, 0f)
        assertEquals(100f * Sin45, result)
    }

    @Test
    fun computeRotatedHeaderHeightPx_withBothZero_returnsZero() {
        assertEquals(0f, computeRotatedHeaderHeightPx(0f, 0f))
    }
}
