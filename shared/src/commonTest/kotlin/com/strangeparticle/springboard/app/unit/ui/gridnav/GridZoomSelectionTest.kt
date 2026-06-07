package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.percent
import kotlin.test.Test
import kotlin.test.assertEquals

class GridZoomSelectionTest {
    @Test
    fun defaultIsOneHundredPercent() {
        assertEquals(100, GridZoomSelection.default().percent)
    }

    @Test
    fun fromPercentRoundTripsThroughPercent() {
        val zoom = GridZoomSelection.fromPercent(175)
        assertEquals(175, zoom.percent)
    }
}
