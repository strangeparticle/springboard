package com.strangeparticle.springboard.app.ai.core

import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import com.strangeparticle.springboard.app.domain.factory.springboardToDto
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.serialization.Serializable

/**
 * A point-in-time picture of all open tabs. Used as:
 *
 * 1. The `tool_result` payload returned to the model after every tool call.
 * 2. A synthetic snapshot message injected into conversation history when
 *    [com.strangeparticle.springboard.app.ai.core.AiSessionManager.stateChangedSinceLastSnapshotSent]
 *    is true (so the model sees the latest state on the very next request).
 * 3. The initial state visible to the model when the chat first starts.
 *
 * Per spec §3.2.
 */
@Serializable
internal data class AppStateSnapshot(
    val tabs: List<TabSnapshot>,
    val activeTabId: String?,
    val lastError: AppStateError? = null,
) {

    companion object {
        /**
         * Builds a snapshot from the current viewmodel state. Each open tab is
         * captured; tabs without a loaded springboard appear with `springboard = null`
         * so the model can distinguish "empty tab waiting for an open" from "this
         * tab has data."
         */
        fun capture(viewModel: SpringboardViewModel): AppStateSnapshot {
            val tabSnapshots = viewModel.tabs.map { tab ->
                TabSnapshot(
                    tabId = tab.tabId,
                    label = tab.label,
                    source = tab.source,
                    isDirty = tab.isDirty,
                    springboard = tab.springboard?.let(::springboardToDto),
                )
            }
            return AppStateSnapshot(
                tabs = tabSnapshots,
                activeTabId = viewModel.activeTabId.takeIf { activeId ->
                    viewModel.tabs.any { it.tabId == activeId }
                },
                lastError = null,
            )
        }
    }
}

/**
 * One tab inside an [AppStateSnapshot]. `springboard` is null for tabs that haven't
 * loaded a springboard yet (a freshly-created empty tab, or a tab whose load failed).
 */
@Serializable
internal data class TabSnapshot(
    val tabId: String,
    val label: String,
    val source: String?,
    val isDirty: Boolean,
    val springboard: SpringboardDto?,
)

/**
 * Structured error attached to the last operation reflected in the snapshot. Set when
 * a tool call failed (e.g. precondition violation, write failure) so the model can
 * see what went wrong without us mixing prose into the snapshot payload.
 */
@Serializable
internal data class AppStateError(
    val message: String,
    val code: String,
)
