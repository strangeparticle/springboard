package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.viewmodel.TabState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabStateTest {
    @Test
    fun createEmptyProducesDefaultEmptyTab() {
        val tab = TabState.createEmpty("tab-1")
        assertEquals("tab-1", tab.tabId)
        assertEquals(TabState.DEFAULT_EMPTY_LABEL, tab.label)
        assertNull(tab.source)
        assertNull(tab.springboard)
        assertNull(tab.selectedEnvironmentId)
        assertNull(tab.selectedAppId)
        assertNull(tab.selectedResourceId)
        assertTrue(tab.multiSelectSet.isEmpty())
        assertNull(tab.hoveredActivatorPreview)
        assertEquals(GridZoomSelection.default(), tab.gridZoomSelection)
        assertFalse(tab.isLoading)
    }

    @Test
    fun isEmptyIsTrueWhenSpringboardIsNull() {
        val tab = TabState.createEmpty("x")
        assertTrue(tab.isEmpty)
    }
}
