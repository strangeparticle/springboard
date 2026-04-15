package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.viewmodel.DEFAULT_EMPTY_TAB_LABEL
import com.strangeparticle.springboard.app.viewmodel.TAB_LABEL_MAX_LENGTH
import com.strangeparticle.springboard.app.viewmodel.deriveTabLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabLabelDerivationTest {
    @Test
    fun filePathUsesFilenameWithoutExtension() {
        assertEquals("my-project", deriveTabLabel("/Users/grey/Documents/my-project.json"))
    }

    @Test
    fun filePathWithoutExtensionUsesFullBaseName() {
        assertEquals("config", deriveTabLabel("/Users/grey/config"))
    }

    @Test
    fun urlUsesLastPathSegmentWithoutExtension() {
        assertEquals("team-tools", deriveTabLabel("https://example.com/api/springboards/team-tools.json"))
    }

    @Test
    fun urlWithTrailingSlashFallsBackToHost() {
        assertEquals("example.com", deriveTabLabel("https://example.com/"))
    }

    @Test
    fun urlWithNoPathFallsBackToHost() {
        assertEquals("example.com", deriveTabLabel("https://example.com"))
    }

    @Test
    fun longLabelIsTruncatedWithEllipsis() {
        val longName = "a".repeat(80)
        val result = deriveTabLabel("/Users/grey/$longName.json")
        assertEquals(TAB_LABEL_MAX_LENGTH, result.length)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun nullSourceReturnsDefaultEmptyLabel() {
        assertEquals(DEFAULT_EMPTY_TAB_LABEL, deriveTabLabel(null))
    }

    @Test
    fun blankSourceReturnsDefaultEmptyLabel() {
        assertEquals(DEFAULT_EMPTY_TAB_LABEL, deriveTabLabel("   "))
    }
}
