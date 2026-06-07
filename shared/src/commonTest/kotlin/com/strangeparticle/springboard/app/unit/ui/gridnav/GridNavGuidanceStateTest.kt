package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.ui.gridnav.GridNavGuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GridNavGuidanceStateTest {

    private val testDismissDelay = 50L
    private val coordA = Coordinate("env1", "app1", "res1")
    private val coordB = Coordinate("env1", "app2", "res1")

    @Test
    fun showGuidance_setsActiveCoordinate() {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        assertEquals(coordA, state.activeCoordinate.value)
    }

    @Test
    fun clearGuidance_removesActiveCoordinate() {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        state.clearGuidance()
        assertNull(state.activeCoordinate.value)
    }

    @Test
    fun beginDismiss_clearsAfterDelay() = runTest {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        state.beginDismiss(this, coordA)

        // Still active before delay elapses
        assertEquals(coordA, state.activeCoordinate.value)

        advanceTimeBy(testDismissDelay + 1)
        assertNull(state.activeCoordinate.value)
    }

    @Test
    fun cancelDismiss_preventsDelayedClear() = runTest {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        state.beginDismiss(this, coordA)
        state.cancelDismiss()

        advanceTimeBy(testDismissDelay + 1)
        // Still active because dismiss was cancelled
        assertEquals(coordA, state.activeCoordinate.value)
    }

    @Test
    fun showGuidance_cancelsExistingDismiss() = runTest {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        state.beginDismiss(this, coordA)

        // Show a different coordinate before dismiss fires
        state.showGuidance(coordB)

        advanceTimeBy(testDismissDelay + 1)
        // coordB is active, coordA's dismiss was cancelled by showGuidance
        assertEquals(coordB, state.activeCoordinate.value)
    }

    @Test
    fun beginDismiss_doesNotClearDifferentCoordinate() = runTest {
        val state = GridNavGuidanceState(dismissDelayMs = testDismissDelay)
        state.showGuidance(coordA)
        state.beginDismiss(this, coordA)

        // Switch to coordB before dismiss fires
        state.showGuidance(coordB)
        // Cancel the pending dismiss (simulating re-entering tooltip)
        state.cancelDismiss()

        // Start a new dismiss for coordA (which is no longer active)
        state.beginDismiss(this, coordA)
        advanceTimeBy(testDismissDelay + 1)

        // coordB should still be active because the dismiss was for coordA
        assertEquals(coordB, state.activeCoordinate.value)
    }
}
