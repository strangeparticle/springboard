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
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.viewmodel.SaveResult

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

    private fun createViewModel() = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())

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
    fun selectPreviousTabWrapsFromFirstToLast() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        val thirdId = viewModel.createTab()!!
        viewModel.selectTab(firstId)
        viewModel.selectPreviousTab()
        assertEquals(thirdId, viewModel.activeTabId)
        viewModel.selectTab(secondId)
        viewModel.selectPreviousTab()
        assertEquals(firstId, viewModel.activeTabId)
    }

    @Test
    fun selectNextTabWrapsFromLastToFirst() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        val secondId = viewModel.createTab()!!
        val thirdId = viewModel.createTab()!!
        viewModel.selectTab(thirdId)
        viewModel.selectNextTab()
        assertEquals(firstId, viewModel.activeTabId)
        viewModel.selectNextTab()
        assertEquals(secondId, viewModel.activeTabId)
    }

    @Test
    fun cycleHelpersAreNoopWithSingleTab() {
        val viewModel = createViewModel()
        val onlyId = viewModel.activeTabId
        viewModel.selectPreviousTab()
        assertEquals(onlyId, viewModel.activeTabId)
        viewModel.selectNextTab()
        assertEquals(onlyId, viewModel.activeTabId)
    }

    @Test
    fun loadConfigInNewTabCreatesTabAndLoadsIntoIt() {
        val viewModel = createViewModel()
        val firstId = viewModel.activeTabId
        viewModel.loadConfigInNewTab(validJson, "/tmp/x.json")
        assertEquals(2, viewModel.tabs.size)
        assertNotEquals(firstId, viewModel.activeTabId)
        assertNotNull(viewModel.springboardFilteredForRuntime)
        assertEquals("Test", viewModel.springboardFilteredForRuntime?.name)
    }

    @Test
    fun loadConfigInNewTabPersistsTabsDto() {
        val persistence = PersistenceServiceInMemoryFake()
        val settingsManager = SettingsManager(RuntimeEnvironment.Test, com.strangeparticle.springboard.app.shared.createSettingsRegistryForTest(), persistence).also { it.loadSettingsAtStartup() }
        val viewModel = SpringboardViewModel(settingsManager, persistence)
        viewModel.loadConfigInNewTab(validJson, "/tmp/x.json")
        val persisted = persistence.loadTabs()
        assertNotNull(persisted)
        assertEquals(1, persisted.tabs.size)
        val entry = persisted.tabs.single()
        assertEquals("/tmp/x.json", entry.source)
        assertEquals(entry.tabId, persisted.activeTabId)
        assertEquals(viewModel.activeTabId, persisted.activeTabId)
    }

    @Test
    fun createUnsavedSpringboardTabCreatesDirtySourceLessUntitledSpringboard() {
        val viewModel = createViewModel()

        val result = viewModel.createUnsavedSpringboardTab()

        assertTrue(result is SpringboardViewModel.LoadResult.Success)
        val activeTab = viewModel.activeTab
        assertNotNull(activeTab)
        assertEquals("Untitled-1", activeTab.springboardFilteredForRuntime?.name)
        assertEquals("Untitled-1", activeTab.label)
        assertNull(activeTab.source)
        assertTrue(activeTab.isDirty)
        assertNull(activeTab.selectedEnvironmentId)
        assertNull(activeTab.selectedAppId)
        assertNull(activeTab.selectedResourceId)
        assertTrue(activeTab.multiSelectSet.isEmpty())
        assertNull(activeTab.hoveredActivatorPreview)
        assertEquals("", activeTab.springboardFilteredForRuntime?.source)
        assertEquals("", activeTab.springboardFilteredForRuntime?.jsonSource)
    }

    @Test
    fun createUnsavedSpringboardTabUsesNextUntitledNameFromOpenSpringboards() {
        val viewModel = createViewModel()

        viewModel.createUnsavedSpringboardTab()
        viewModel.createUnsavedSpringboardTab()

        assertEquals("Untitled-2", viewModel.activeTab?.springboardFilteredForRuntime?.name)
    }

    @Test
    fun createUnsavedSpringboardTabIgnoresEmptyTabsWhenGeneratingName() {
        val viewModel = createViewModel()

        viewModel.createTab()
        viewModel.createUnsavedSpringboardTab()

        assertEquals("Untitled-1", viewModel.activeTab?.springboardFilteredForRuntime?.name)
    }

    @Test
    fun sourceLessUnsavedSpringboardCannotSaveInPlaceButCanSaveAs() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val viewModel = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            fileContentService = fileService,
        )
        viewModel.createUnsavedSpringboardTab()

        assertEquals(SaveResult.NotSupportedForSource, viewModel.saveActiveTab())

        val saveAsResult = viewModel.saveActiveTabAs("/tmp/untitled.json")
        assertEquals(SaveResult.Success("/tmp/untitled.json"), saveAsResult)
        assertEquals("/tmp/untitled.json", viewModel.activeTab?.source)
        assertEquals(false, viewModel.activeTab?.isDirty)
        assertTrue(fileService.writtenFiles.containsKey("/tmp/untitled.json"))
    }
}
