package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.strangeparticle.springboard.app.domain.model.Coordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle of the single active guidance tooltip in the grid.
 * Only one tooltip can be visible at a time. Dismissal uses a configurable delay
 * so the tooltip stays visible briefly after the pointer leaves, allowing the user
 * to hover into the tooltip itself.
 */
class GridNavGuidanceState(
    private val dismissDelayMs: Long = GuidanceDismissDelayMs,
) {
    var activeCoordinate = mutableStateOf<Coordinate?>(null)
        private set

    private var dismissJob = mutableStateOf<Job?>(null)

    /** Show guidance for [coordinate], cancelling any pending dismiss. */
    fun showGuidance(coordinate: Coordinate) {
        dismissJob.value?.cancel()
        dismissJob.value = null
        activeCoordinate.value = coordinate
    }

    /**
     * Begin a delayed dismiss for [coordinate]. If the pointer re-enters the cell or
     * tooltip before the delay elapses, call [cancelDismiss] to keep it visible.
     */
    fun beginDismiss(scope: CoroutineScope, coordinate: Coordinate) {
        dismissJob.value?.cancel()
        dismissJob.value = scope.launch {
            delay(dismissDelayMs)
            if (activeCoordinate.value == coordinate) {
                activeCoordinate.value = null
            }
        }
    }

    /** Cancel a pending dismiss, keeping the current tooltip visible. */
    fun cancelDismiss() {
        dismissJob.value?.cancel()
        dismissJob.value = null
    }

    /** Immediately clear the active tooltip with no delay. */
    fun clearGuidance() {
        dismissJob.value?.cancel()
        dismissJob.value = null
        activeCoordinate.value = null
    }
}

@Composable
fun rememberGridNavGuidanceState(): GridNavGuidanceState =
    remember { GridNavGuidanceState() }
