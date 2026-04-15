package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.MAX_OPEN_TABS
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpringboardViewModelTabsTest {

    private val validJson = """
    {
      "name": "Test",
      "environments": [{ "id": "prod", "name": "Prod" }],
      "apps": [{ "id": "app1", "name": "App One" }],
      "resources": [{ "id": "res1", "name": "Res One" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com" }
      ]
    }
    """.trimIndent()

    private fun createViewModel() = SpringboardViewModel(createSettingsManagerForTest())

    @Test
    fun initialViewModelHasOneEmptyActiveTab() {
        val viewModel = createViewModel()
        assertEquals(1, viewModel.tabs.size)
        assertEquals(viewModel.tabs.first().tabId, viewModel.activeTabId)
        assertTrue(viewModel.tabs.first().isEmpty)
    }

    @Test
    fun createTabIncrementsCountAndActivatesNewTab() {
        val viewModel = createViewModel()
        val originalActive = viewModel.activeTabId
        val newId = viewModel.createTab()
        assertNotNull(newId)
        assertEquals(2, viewModel.tabs.size)
        assertEquals(newId, viewModel.activeTabId)
        assertNotEquals(originalActive, newId)
    }

    @Test
    fun createTabAtLimitReturnsNullAndLeavesStateUnchanged() {
        val viewModel = createViewModel()
        repeat(MAX_OPEN_TABS - 1) { viewModel.createTab() }
        assertEquals(MAX_OPEN_TABS, viewModel.tabs.size)
        assertFalse(viewModel.canCreateNewTab)
        val activeBefore = viewModel.activeTabId
        val result = viewModel.createTab()
        assertNull(result)
        assertEquals(MAX_OPEN_TABS, viewModel.tabs.size)
        assertEquals(activeBefore, viewModel.activeTabId)
    }

    @Test
    fun canCreateNewTabIsTrueBelowLimit() {
        val viewModel = createViewModel()
        assertTrue(viewModel.canCreateNewTab)
    }

    @Test
    fun selectTabChangesActiveTab() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        viewModel.selectTab(firstId)
        assertEquals(firstId, viewModel.activeTabId)
        viewModel.selectTab(secondId)
        assertEquals(secondId, viewModel.activeTabId)
    }

    @Test
    fun closeTabSelectsAdjacentTabToTheRight() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        val thirdId = viewModel.createTab()!!
        viewModel.selectTab(secondId)
        viewModel.closeTab(secondId)
        assertEquals(2, viewModel.tabs.size)
        assertEquals(thirdId, viewModel.activeTabId)
        assertTrue(viewModel.tabs.none { it.tabId == secondId })
        // sanity: firstId still present
        assertTrue(viewModel.tabs.any { it.tabId == firstId })
    }

    @Test
    fun closeActiveRightmostTabSelectsTabToTheLeft() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        viewModel.selectTab(secondId)
        viewModel.closeTab(secondId)
        assertEquals(1, viewModel.tabs.size)
        assertEquals(firstId, viewModel.activeTabId)
    }

    @Test
    fun closeNonActiveTabLeavesActiveUnchanged() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        viewModel.selectTab(firstId)
        viewModel.closeTab(secondId)
        assertEquals(1, viewModel.tabs.size)
        assertEquals(firstId, viewModel.activeTabId)
    }

    @Test
    fun closeLastTabProducesFreshEmptyTab() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        viewModel.loadConfig(validJson, "/tmp/a.json")
        viewModel.closeTab(firstId)
        assertEquals(1, viewModel.tabs.size)
        val remaining = viewModel.tabs.first()
        assertTrue(remaining.isEmpty)
        assertEquals(remaining.tabId, viewModel.activeTabId)
        assertNotEquals(firstId, remaining.tabId)
    }

    @Test
    fun loadConfigInNewTabCreatesTabAndLoadsIntoIt() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        viewModel.loadConfigInNewTab(validJson, "/tmp/x.json")
        assertEquals(2, viewModel.tabs.size)
        assertNotEquals(firstId, viewModel.activeTabId)
        assertNotNull(viewModel.springboard)
        assertEquals("Test", viewModel.springboard?.name)
    }
}
