package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.formatTimestamp
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampFormattingTest {

    @Test
    fun `timestamp is formatted in desktop style`() {
        assertEquals(
            "Apr 6, 2026, 6:08:58 PM",
            formatTimestamp(millis = 1775498938000, timeZone = TimeZone.UTC),
        )
    }
}
