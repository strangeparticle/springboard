package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.domain.undo.UndoRedoOutcome
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SaveResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpringboardUndoRedoTest {

    private fun createViewModel() =
        SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())

    private fun createViewModelWithFileService(
        fileService: PlatformFileContentServiceInMemoryFake,
    ) = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(),
        persistenceService = PersistenceServiceInMemoryFake(),
        fileContentService = fileService,
    )

    /**
     * Loads [TestFixtureJson.ALTERNATIVE_URL_ONLY] into a throwaway second tab and returns its
     * unfiltered springboard, then restores the originally-active tab. Gives tests a distinct
     * springboard instance to feed [SpringboardViewModel.commitTabEdit] without disturbing the
     * tab under test.
     */
    private fun captureDistinctAlternativeSpringboard(viewModel: SpringboardViewModel) =
        captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)

    private fun captureDistinctSpringboard(
        viewModel: SpringboardViewModel,
        configJson: String,
    ): com.strangeparticle.springboard.app.domain.model.Springboard {
        val originalActiveTabId = viewModel.activeTabId
        val scratchTabId = viewModel.createTab()!!
        viewModel.loadConfig(configJson, "/tmp/scratch-${scratchTabId}.json")
        val captured = viewModel.springboardUnfiltered!!
        viewModel.closeTab(scratchTabId)
        viewModel.selectTab(originalActiveTabId)
        return captured
    }

    @Test
    fun initiallyCannotUndoOrRedo() {
        val viewModel = createViewModel()
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        assertFalse(viewModel.canUndoActiveTab)
        assertFalse(viewModel.canRedoActiveTab)
    }

    @Test
    fun afterOneEditCanUndoAndTabIsDirty() {
        val viewModel = createViewModel()
        val firstTabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.selectTab(firstTabId)
        viewModel.commitTabEdit(firstTabId, springboardB)

        assertTrue(viewModel.canUndoActiveTab)
        assertEquals(true, viewModel.activeTab?.isDirty)
    }

    @Test
    fun undoRestoresPreviousSpringboardAndClearsDirty() {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardA = viewModel.springboardUnfiltered!!
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.commitTabEdit(tabId, springboardB)
        val outcome = viewModel.undoActiveTab()

        assertEquals(UndoRedoOutcome.Undone, outcome)
        assertSame(springboardA, viewModel.activeTab?.springboardUnfiltered)
        assertTrue(viewModel.canRedoActiveTab)
        assertEquals(false, viewModel.activeTab?.isDirty)
    }

    @Test
    fun redoReappliesEdit() {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.commitTabEdit(tabId, springboardB)
        viewModel.undoActiveTab()
        val outcome = viewModel.redoActiveTab()

        assertEquals(UndoRedoOutcome.Redone, outcome)
        assertSame(springboardB, viewModel.activeTab?.springboardUnfiltered)
        assertEquals(true, viewModel.activeTab?.isDirty)
    }

    @Test
    fun undoWithNoEditsReturnsNothingToUndo() {
        val viewModel = createViewModel()
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardA = viewModel.springboardUnfiltered!!

        val outcome = viewModel.undoActiveTab()

        assertEquals(UndoRedoOutcome.NothingToUndo, outcome)
        assertSame(springboardA, viewModel.activeTab?.springboardUnfiltered)
        assertFalse(viewModel.canUndoActiveTab)
    }

    @Test
    fun perTabIsolation() {
        val viewModel = createViewModel()
        val firstTabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.selectTab(firstTabId)
        viewModel.commitTabEdit(firstTabId, springboardB)

        val secondTabId = viewModel.createTab()!!
        assertEquals(secondTabId, viewModel.activeTabId)
        assertFalse(viewModel.canUndoActiveTab)

        viewModel.selectTab(firstTabId)
        assertTrue(viewModel.canUndoActiveTab)
    }

    @Test
    fun transactionCoalescesMultipleEditsIntoOneStep() {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardA = viewModel.springboardUnfiltered!!
        val springboardB = captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)
        val springboardC = captureDistinctSpringboard(viewModel, TestFixtureJson.MULTI_ENV_WITH_COMMON)

        viewModel.beginEditTransaction()
        viewModel.commitTabEdit(tabId, springboardB)
        viewModel.commitTabEdit(tabId, springboardC)
        viewModel.commitEditTransaction()

        val outcome = viewModel.undoActiveTab()
        assertEquals(UndoRedoOutcome.Undone, outcome)
        assertSame(springboardA, viewModel.activeTab?.springboardUnfiltered)
        assertFalse(viewModel.canUndoActiveTab)
    }

    @Test
    fun savingMarksHistorySavedSoUndoToSavedPointIsClean() = runTest {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val viewModel = createViewModelWithFileService(fileService)
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/t.json")
        val springboardB = captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)

        viewModel.commitTabEdit(tabId, springboardB)
        assertEquals(true, viewModel.activeTab?.isDirty)

        val saveResult = viewModel.saveTab(tabId)
        assertTrue(saveResult is SaveResult.Success)
        assertEquals(false, viewModel.activeTab?.isDirty)

        val springboardC = captureDistinctSpringboard(viewModel, TestFixtureJson.MULTI_ENV_WITH_COMMON)
        viewModel.commitTabEdit(tabId, springboardC)
        assertEquals(true, viewModel.activeTab?.isDirty)

        val undoOutcome = viewModel.undoActiveTab()
        assertEquals(UndoRedoOutcome.Undone, undoOutcome)
        assertSame(springboardB, viewModel.activeTab?.springboardUnfiltered)
        assertEquals(false, viewModel.activeTab?.isDirty)
    }

    @Test
    fun reloadingConfigResetsHistory() {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.selectTab(tabId)
        viewModel.commitTabEdit(tabId, springboardB)
        assertTrue(viewModel.canUndoActiveTab)

        // Reloading a fresh config into the same tab re-installs the springboard,
        // which resets the tab's history.
        viewModel.loadConfig(TestFixtureJson.ALTERNATIVE_URL_ONLY, "/tmp/a.json")
        assertFalse(viewModel.canUndoActiveTab)
    }

    @Test
    fun closingTabDropsHistoryForThatTab() {
        val viewModel = createViewModel()
        val firstTabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.selectTab(firstTabId)
        viewModel.commitTabEdit(firstTabId, springboardB)
        assertTrue(viewModel.canUndoActiveTab)

        viewModel.createTab()!!
        viewModel.selectTab(firstTabId)
        viewModel.closeTab(firstTabId)

        // The closed tab's history is gone and the remaining tab has none of its own.
        assertFalse(viewModel.canUndoActiveTab)
    }

    @Test
    fun closingTheOnlyTabLeavesNoUndoHistory() {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/tmp/a.json")
        val springboardB = captureDistinctAlternativeSpringboard(viewModel)

        viewModel.selectTab(tabId)
        viewModel.commitTabEdit(tabId, springboardB)
        assertTrue(viewModel.canUndoActiveTab)

        // Closing the only tab replaces it in-place with a fresh empty tab, whose
        // (absent) history must not surface the closed tab's leftover undo steps.
        viewModel.closeTab(viewModel.activeTabId)
        assertFalse(viewModel.canUndoActiveTab)
    }
}
