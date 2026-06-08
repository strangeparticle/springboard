package com.strangeparticle.springboard.app.unit.undo

import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.undo.SpringboardEditHistory
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpringboardEditHistoryTest {

    private fun createViewModel() =
        SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())

    private fun loadSpringboard(viewModel: SpringboardViewModel, json: String, source: String): Springboard {
        viewModel.loadConfig(json, source)
        return checkNotNull(viewModel.springboardUnfiltered) { "springboardUnfiltered was null after loadConfig" }
    }

    // Returns four distinct Springboard instances by loading different configs into separate ViewModels.
    private fun fourDistinctSpringboards(): List<Springboard> {
        val vmA = createViewModel()
        val springboardA = loadSpringboard(vmA, TestFixtureJson.URL_ONLY, "/a.json")

        val vmB = createViewModel()
        val springboardB = loadSpringboard(vmB, TestFixtureJson.ALTERNATIVE_URL_ONLY, "/b.json")

        val vmC = createViewModel()
        val springboardC = loadSpringboard(vmC, TestFixtureJson.COMMAND_ACTIVATOR, "/c.json")

        val vmD = createViewModel()
        val springboardD = loadSpringboard(vmD, TestFixtureJson.TERMINAL_ACTIVATOR, "/d.json")

        return listOf(springboardA, springboardB, springboardC, springboardD)
    }

    @Test
    fun freshHistoryCannotUndoOrRedo() {
        val (springboardA) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertSame(springboardA, history.current)
        assertFalse(history.isDirty)
    }

    @Test
    fun afterRecordCanUndoButNotRedo() {
        val (springboardA, springboardB) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)

        history.record(springboardB)

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertSame(springboardB, history.current)
        assertTrue(history.isDirty)
    }

    @Test
    fun undoAfterRecordReturnsPreviousAndEnablesRedo() {
        val (springboardA, springboardB) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)
        history.record(springboardB)

        val undoResult = history.undo()

        assertSame(springboardA, undoResult)
        assertSame(springboardA, history.current)
        assertTrue(history.canRedo)
        // Cursor is back at index 0, which is the savedMarker, so not dirty.
        assertFalse(history.isDirty)
    }

    @Test
    fun redoAfterUndoReturnsBAndMarksDirty() {
        val (springboardA, springboardB) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)
        history.record(springboardB)
        history.undo()

        val redoResult = history.redo()

        assertSame(springboardB, redoResult)
        assertSame(springboardB, history.current)
        assertTrue(history.isDirty)
    }

    @Test
    fun recordAfterUndoInvalidatesRedoBranch() {
        val (springboardA, springboardB, springboardC) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)
        history.record(springboardB)
        history.undo()

        history.record(springboardC)

        assertFalse(history.canRedo)
        assertSame(springboardC, history.current)
        assertTrue(history.isDirty)
    }

    @Test
    fun markSavedShiftsSavedMarkerSoUndoBecomesCleanThenDirty() {
        val (springboardA, springboardB) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)
        history.record(springboardB)

        // Mark the new state (index 1) as saved.
        history.markSaved()
        assertFalse(history.isDirty)

        // Undo back to springboardA (index 0) — now dirty because saved marker is at 1.
        history.undo()
        assertTrue(history.isDirty)
    }

    @Test
    fun undoAtBaseReturnsNullAndLeavesCurrentUnchanged() {
        val (springboardA) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)

        val result = history.undo()

        assertNull(result)
        assertSame(springboardA, history.current)
    }

    @Test
    fun redoAtTipReturnsNull() {
        val (springboardA, springboardB) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 100)
        history.record(springboardB)

        val result = history.redo()

        assertNull(result)
        assertSame(springboardB, history.current)
    }

    @Test
    fun capEvictsOldestAndOldestIsNoLongerReachable() {
        // Build a history with maxEntries = 3.
        // Load: initial (sbA), then record sbB, sbC, sbD — four states, but cap is 3.
        // After eviction the oldest (sbA) is gone; the retained states are sbB, sbC, sbD.
        val (springboardA, springboardB, springboardC, springboardD) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 3)

        history.record(springboardB)
        history.record(springboardC)
        history.record(springboardD)

        // Current should be the latest recorded snapshot.
        assertSame(springboardD, history.current)
        assertTrue(history.canUndo)

        // Undo all the way to the bottom — should land at sbB, not sbA.
        history.undo() // -> sbC
        history.undo() // -> sbB
        val bottomResult = history.undo() // sbB is the oldest retained; another undo returns null
        assertNull(bottomResult)
        assertSame(springboardB, history.current)
        assertFalse(history.canUndo)
    }

    @Test
    fun capEvictionWithSavedMarkerAtEvictedIndexSetsMarkerToNull() {
        // savedMarker starts at 0 (pointing to sbA); after eviction sbA is gone,
        // so isDirty should be true even at the oldest retained snapshot.
        val (springboardA, springboardB, springboardC, springboardD) = fourDistinctSpringboards()
        val history = SpringboardEditHistory(initial = springboardA, maxEntries = 3)

        history.record(springboardB)
        history.record(springboardC)
        history.record(springboardD)

        // Undo to the bottom of what's retained.
        history.undo() // -> sbC
        history.undo() // -> sbB

        // savedMarker was at index 0 (sbA), which was evicted, so it's null → isDirty.
        assertTrue(history.isDirty)
    }

    @Test
    fun constructingWithMaxEntriesZeroThrows() {
        val (springboardA) = fourDistinctSpringboards()

        assertFailsWith<IllegalArgumentException> {
            SpringboardEditHistory(initial = springboardA, maxEntries = 0)
        }
    }
}
