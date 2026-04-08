package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.ui.gridnav.GridNavHeaderIdTextSizeSp
import com.strangeparticle.springboard.app.ui.gridnav.truncateHeaderText
import com.strangeparticle.springboard.app.ui.gridnav.truncateHeaderTextToFitWidth

import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderTextUtilsTest {

    @Test
    fun `grid nav header id text size is 8sp`() {
        assertEquals(8, GridNavHeaderIdTextSizeSp)
    }

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

    // ----- truncateHeaderTextToFitWidth (height/width-aware) -----

    /** Fake measurer: each character is 1px wide. The ellipsis "…" is also 1px. */
    private val onePxPerChar: (String) -> Float = { it.length.toFloat() }

    @Test
    fun `fit width returns full text when it fits`() {
        assertEquals(
            "hello",
            truncateHeaderTextToFitWidth("hello", maxWidthPx = 5f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width returns full text when budget exceeds need`() {
        assertEquals(
            "hello",
            truncateHeaderTextToFitWidth("hello", maxWidthPx = 50f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width truncates with ellipsis when text exceeds budget`() {
        // budget 4 → "abc…" (4 chars including ellipsis) fits, "abcd…" (5) does not
        assertEquals(
            "abc…",
            truncateHeaderTextToFitWidth("abcdefg", maxWidthPx = 4f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width returns lone ellipsis when budget is 1`() {
        assertEquals(
            "…",
            truncateHeaderTextToFitWidth("abcdef", maxWidthPx = 1f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width returns lone ellipsis when budget is zero`() {
        assertEquals(
            "…",
            truncateHeaderTextToFitWidth("abcdef", maxWidthPx = 0f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width returns empty when input is empty`() {
        assertEquals(
            "",
            truncateHeaderTextToFitWidth("", maxWidthPx = 10f, measureWidthPx = onePxPerChar),
        )
    }

    @Test
    fun `fit width reveals more characters as budget grows`() {
        val text = "feburil-hoodymap-lucky"
        val small = truncateHeaderTextToFitWidth(text, maxWidthPx = 5f, measureWidthPx = onePxPerChar)
        val medium = truncateHeaderTextToFitWidth(text, maxWidthPx = 10f, measureWidthPx = onePxPerChar)
        val large = truncateHeaderTextToFitWidth(text, maxWidthPx = 100f, measureWidthPx = onePxPerChar)
        assertEquals("febu…", small)
        assertEquals("feburil-h…", medium)
        assertEquals("feburil-hoodymap-lucky", large)
    }
}
