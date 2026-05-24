package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.domain.model.AppColumn
import com.strangeparticle.springboard.app.domain.model.AppGroupColumnSpan
import com.strangeparticle.springboard.app.domain.model.SeparatorColumn
import com.strangeparticle.springboard.app.ui.gridnav.GridNavGroupLabelHighlight
import com.strangeparticle.springboard.app.ui.gridnav.appIdAtGroupLabelStripPointer
import com.strangeparticle.springboard.app.ui.gridnav.resolveGroupLabelHighlight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GridNavGroupLabelStripTest {
    private val slots = listOf(
        AppColumn(App("app1", "One", appGroupId = "groupA")),
        AppColumn(App("app2", "Two", appGroupId = "groupA")),
        SeparatorColumn,
        AppColumn(App("app3", "Three")),
    )
    private val groupSpans = listOf(
        AppGroupColumnSpan("groupA", "Group A", startSlotIndex = 0, columnCount = 2),
    )

    @Test
    fun `grouped app hover highlights full group span`() {
        assertEquals(
            GridNavGroupLabelHighlight(startSlotIndex = 0, columnCount = 2),
            resolveGroupLabelHighlight(slots, groupSpans, hoveredAppId = "app2", hoveredHeaderAppId = null),
        )
    }

    @Test
    fun `ungrouped app hover highlights only its column`() {
        assertEquals(
            GridNavGroupLabelHighlight(startSlotIndex = 3, columnCount = 1),
            resolveGroupLabelHighlight(slots, groupSpans, hoveredAppId = "app3", hoveredHeaderAppId = null),
        )
    }

    @Test
    fun `header hover participates in group strip highlight`() {
        assertEquals(
            GridNavGroupLabelHighlight(startSlotIndex = 0, columnCount = 2),
            resolveGroupLabelHighlight(slots, groupSpans, hoveredAppId = null, hoveredHeaderAppId = "app1"),
        )
    }

    @Test
    fun `unknown hover has no strip highlight`() {
        assertNull(resolveGroupLabelHighlight(slots, groupSpans, hoveredAppId = "missing", hoveredHeaderAppId = null))
    }

    @Test
    fun `strip pointer maps app slots to app ids`() {
        assertEquals("app1", appIdAtGroupLabelStripPointer(x = 10f, columnWidthPx = 50f, slots = slots))
        assertEquals("app2", appIdAtGroupLabelStripPointer(x = 60f, columnWidthPx = 50f, slots = slots))
        assertEquals("app3", appIdAtGroupLabelStripPointer(x = 160f, columnWidthPx = 50f, slots = slots))
    }

    @Test
    fun `strip pointer ignores separator slots and out of range positions`() {
        assertNull(appIdAtGroupLabelStripPointer(x = 110f, columnWidthPx = 50f, slots = slots))
        assertNull(appIdAtGroupLabelStripPointer(x = -1f, columnWidthPx = 50f, slots = slots))
        assertNull(appIdAtGroupLabelStripPointer(x = 250f, columnWidthPx = 50f, slots = slots))
    }
}
