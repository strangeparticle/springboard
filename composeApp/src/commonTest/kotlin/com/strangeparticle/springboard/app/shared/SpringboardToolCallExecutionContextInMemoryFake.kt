package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.editio.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.CompletableDeferred

/**
 * Test implementation of [SpringboardToolCallExecutionContext]. Holds a real [SpringboardViewModel]
 * (needed because [com.strangeparticle.springboard.app.editio.SpringboardAppSnapshot.capture]
 * reads viewmodel state when a tool builds its result). Records `markStateChanged()`
 * calls and exposes the approval-deferred registry so tests can drive confirmation
 * flows.
 */
internal class SpringboardToolCallExecutionContextInMemoryFake(
    override val viewModel: SpringboardViewModel,
) : SpringboardToolCallExecutionContext {

    /** Number of times [markStateChanged] has been called. */
    var stateChangedCount: Int = 0
        private set

    /** Approval deferreds keyed by tool-call id. Tests resolve these to drive `awaitUserApproval`. */
    val pendingApprovals: MutableMap<String, CompletableDeferred<Boolean>> = mutableMapOf()

    override fun markStateChanged() {
        stateChangedCount += 1
    }

    override suspend fun awaitUserApproval(toolCallId: String): Boolean {
        val deferred = pendingApprovals.getOrPut(toolCallId) { CompletableDeferred() }
        return deferred.await()
    }

    /** Resolve [toolCallId]'s pending approval with [approved]. Tests call this. */
    fun resolveApproval(toolCallId: String, approved: Boolean) {
        val deferred = pendingApprovals.getOrPut(toolCallId) { CompletableDeferred() }
        deferred.complete(approved)
    }
}
