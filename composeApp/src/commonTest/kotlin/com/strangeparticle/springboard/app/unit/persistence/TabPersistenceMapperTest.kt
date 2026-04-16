package com.strangeparticle.springboard.app.unit.persistence

import com.strangeparticle.springboard.app.persistence.buildTabsDto
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.viewmodel.TabState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabPersistenceMapperTest {

    private fun loadedTab(
        tabId: String,
        source: String,
        zoomPercent: Int = 100,
    ): TabState = TabState.createEmpty(tabId).copy(
        source = source,
        gridZoomSelection = GridZoomSelection.fromPercent(zoomPercent),
    )

    @Test
    fun emptyTabsAreExcluded() {
        val tabs = listOf(
            TabState.createEmpty("tab-1"),
            loadedTab("tab-2", "/path/a.json"),
        )
        val dto = buildTabsDto(tabs, activeTabId = "tab-2")
        assertEquals(1, dto.tabs.size)
        assertEquals("tab-2", dto.tabs.first().tabId)
    }

    @Test
    fun persistedFieldsAreCarriedThrough() {
        val tabs = listOf(loadedTab("tab-1", "/path/a.json", zoomPercent = 125))
        val dto = buildTabsDto(tabs, activeTabId = "tab-1")
        val entry = dto.tabs.single()
        assertEquals("tab-1", entry.tabId)
        assertEquals("/path/a.json", entry.source)
        assertEquals(125, entry.zoomPercent)
    }

    @Test
    fun tabOrderIsPreserved() {
        val tabs = listOf(
            loadedTab("tab-1", "/a.json"),
            TabState.createEmpty("tab-2"),
            loadedTab("tab-3", "/c.json"),
            loadedTab("tab-4", "/d.json"),
        )
        val dto = buildTabsDto(tabs, activeTabId = "tab-3")
        assertEquals(listOf("tab-1", "tab-3", "tab-4"), dto.tabs.map { it.tabId })
    }

    @Test
    fun activeTabIdIsNullWhenActiveTabIsEmpty() {
        val tabs = listOf(
            loadedTab("tab-1", "/a.json"),
            TabState.createEmpty("tab-2"),
        )
        val dto = buildTabsDto(tabs, activeTabId = "tab-2")
        assertNull(dto.activeTabId)
    }

    @Test
    fun activeTabIdIsSetWhenActiveTabIsLoaded() {
        val tabs = listOf(loadedTab("tab-1", "/a.json"))
        val dto = buildTabsDto(tabs, activeTabId = "tab-1")
        assertEquals("tab-1", dto.activeTabId)
    }

    @Test
    fun allEmptyTabsProduceEmptyDto() {
        val tabs = listOf(TabState.createEmpty("tab-1"), TabState.createEmpty("tab-2"))
        val dto = buildTabsDto(tabs, activeTabId = "tab-1")
        assertTrue(dto.tabs.isEmpty())
        assertNull(dto.activeTabId)
    }
}
