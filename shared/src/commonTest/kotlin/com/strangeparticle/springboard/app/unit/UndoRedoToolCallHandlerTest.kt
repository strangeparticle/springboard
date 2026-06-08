package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.toolcall.RedoToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.UndoToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [UndoToolCallHandler] and [RedoToolCallHandler], the tool-call surface over the
 * viewmodel's per-tab undo/redo engine.
 */
internal class UndoRedoToolCallHandlerTest {

    private fun createViewModel() =
        SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())

    /**
     * Loads [configJson] into a throwaway scratch tab and returns its unfiltered springboard,
     * then restores the originally-active tab. Gives the test a distinct springboard instance to
     * feed [SpringboardViewModel.commitTabEdit] without disturbing the tab under test.
     */
    private fun captureDistinctSpringboard(
        viewModel: SpringboardViewModel,
        configJson: String,
    ): Springboard {
        val originalActiveTabId = viewModel.activeTabId
        val scratchTabId = viewModel.createTab()!!
        viewModel.loadConfig(configJson, "/tmp/scratch-$scratchTabId.json")
        val captured = viewModel.springboardUnfiltered!!
        viewModel.closeTab(scratchTabId)
        viewModel.selectTab(originalActiveTabId)
        return captured
    }

    @Test
    fun undoRevertsActiveTabAndMarksStateChanged() = runTest {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/t.json")
        val springboardA = viewModel.springboardUnfiltered!!
        val springboardB = captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)
        val context = SpringboardToolCallExecutionContextInMemoryFake(viewModel)

        viewModel.commitTabEdit(tabId, springboardB)
        assertSame(springboardB, viewModel.springboardUnfiltered)

        val response = UndoToolCallHandler().executeToolCallHandler("t1", "{}", context)

        assertIs<SpringboardToolCallHandlerResponse>(response)
        assertTrue(response.success)
        assertEquals("Undid last change.", response.message)
        assertSame(springboardA, viewModel.springboardUnfiltered)
        assertEquals(1, context.stateChangedCount)
    }

    @Test
    fun undoWithNothingToUndoSucceedsWithoutMarkingStateChanged() = runTest {
        val viewModel = createViewModel()
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/t.json")
        val context = SpringboardToolCallExecutionContextInMemoryFake(viewModel)

        val response = UndoToolCallHandler().executeToolCallHandler("t1", "{}", context)

        assertIs<SpringboardToolCallHandlerResponse>(response)
        assertTrue(response.success)
        assertEquals("Nothing to undo.", response.message)
        assertEquals(0, context.stateChangedCount)
    }

    @Test
    fun redoReappliesUndoneChangeAndMarksStateChanged() = runTest {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/t.json")
        val springboardB = captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)
        val context = SpringboardToolCallExecutionContextInMemoryFake(viewModel)

        viewModel.commitTabEdit(tabId, springboardB)
        viewModel.undoActiveTab()

        val response = RedoToolCallHandler().executeToolCallHandler("t1", "{}", context)

        assertIs<SpringboardToolCallHandlerResponse>(response)
        assertTrue(response.success)
        assertEquals("Redid last change.", response.message)
        assertSame(springboardB, viewModel.springboardUnfiltered)
        assertEquals(1, context.stateChangedCount)
    }

    @Test
    fun redoWithNothingToRedoSucceedsWithoutMarkingStateChanged() = runTest {
        val viewModel = createViewModel()
        val tabId = viewModel.activeTabId
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/t.json")
        val springboardB = captureDistinctSpringboard(viewModel, TestFixtureJson.ALTERNATIVE_URL_ONLY)
        val context = SpringboardToolCallExecutionContextInMemoryFake(viewModel)

        // At the tip of history (nothing undone), there is nothing to redo.
        viewModel.commitTabEdit(tabId, springboardB)

        val response = RedoToolCallHandler().executeToolCallHandler("t1", "{}", context)

        assertIs<SpringboardToolCallHandlerResponse>(response)
        assertTrue(response.success)
        assertEquals("Nothing to redo.", response.message)
        assertEquals(0, context.stateChangedCount)
    }
}
