package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.viewmodel.TAB_LABEL_MAX_LENGTH
import com.strangeparticle.springboard.app.viewmodel.deriveTabLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabLabelDerivationTest {
    @Test
    fun shortNameIsReturnedUnchanged() {
        assertEquals("My Springboard", deriveTabLabel("My Springboard"))
    }

    @Test
    fun nameAtMaxLengthIsReturnedUnchanged() {
        val nameAtMax = "a".repeat(TAB_LABEL_MAX_LENGTH)
        assertEquals(nameAtMax, deriveTabLabel(nameAtMax))
    }

    @Test
    fun longNameIsTruncatedWithEllipsis() {
        val longName = "a".repeat(80)
        val result = deriveTabLabel(longName)
        assertEquals(TAB_LABEL_MAX_LENGTH, result.length)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("My Springboard", deriveTabLabel("   My Springboard   "))
    }
}
