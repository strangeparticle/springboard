package com.strangeparticle.springboard.app.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone

class WasmPlatformTimestampFormattingTest {

    @Test
    fun `formatTimestamp matches desktop style instead of raw millis`() {
        assertEquals("Apr 6, 2026, 6:08:58 PM", formatTimestamp(1775498938000, TimeZone.UTC))
    }
}
