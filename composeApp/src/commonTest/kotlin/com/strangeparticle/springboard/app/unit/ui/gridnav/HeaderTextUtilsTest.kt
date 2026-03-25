package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.gridnav.truncateHeaderText

import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderTextUtilsTest {

    @Test
    fun `short text is unchanged`() {
        assertEquals("Springboard", truncateHeaderText("Springboard"))
    }

    @Test
    fun `text at max length is unchanged`() {
        assertEquals("12345678901234567890", truncateHeaderText("12345678901234567890"))
    }

    @Test
    fun `text longer than max length is truncated with ellipsis`() {
        assertEquals("1234567890123456789…", truncateHeaderText("123456789012345678901"))
    }

    @Test
    fun `empty text is unchanged`() {
        assertEquals("", truncateHeaderText(""))
    }
}
