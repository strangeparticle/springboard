package com.strangeparticle.springboard.app.unit.ui.statusbar

import com.strangeparticle.springboard.app.platform.formatTimestamp
import com.strangeparticle.springboard.app.ui.statusbar.statusBarSourceLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusBarSourceLabelTest {

    @Test
    fun blankSourceShowsUnsavedPlaceholder() {
        val label = statusBarSourceLabel(source = "", lastLoadTime = 1_700_000_000_000L)
        assertEquals("<unsaved>", label)
    }

    @Test
    fun whitespaceSourceShowsUnsavedPlaceholder() {
        val label = statusBarSourceLabel(source = "   ", lastLoadTime = 1_700_000_000_000L)
        assertEquals("<unsaved>", label)
    }

    @Test
    fun savedSourceShowsPathWithTimestamp() {
        val lastLoadTime = 1_700_000_000_000L
        val label = statusBarSourceLabel(source = "/test/springboard.json", lastLoadTime = lastLoadTime)
        assertEquals("/test/springboard.json @ ${formatTimestamp(lastLoadTime)}", label)
    }
}
