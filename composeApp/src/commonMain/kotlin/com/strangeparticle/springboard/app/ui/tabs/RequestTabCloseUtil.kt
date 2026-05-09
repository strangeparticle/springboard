package com.strangeparticle.springboard.app.ui.tabs

import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

/**
 * Decides what to do when the user requests to close [tabId].
 *
 * - If the target tab has unsaved in-memory edits ([com.strangeparticle.springboard.app.viewmodel.TabState.isDirty] = true),
 *   [queueConfirmationDialog] is invoked with the tab id so the caller can render
 *   a confirm-before-close dialog before any state mutation happens.
 * - Otherwise the tab is closed immediately via [SpringboardViewModel.closeTab].
 *
 * Pulled out of the [com.strangeparticle.springboard.app.ui.MainScreen] inline `onClose`
 * callback so the decision logic is exercisable without a full Compose host.
 */
internal fun requestTabClose(
    tabId: String,
    viewModel: SpringboardViewModel,
    queueConfirmationDialog: (String) -> Unit,
) {
    val tab = viewModel.tabs.firstOrNull { it.tabId == tabId }
    if (tab?.isDirty == true) {
        queueConfirmationDialog(tabId)
    } else {
        viewModel.closeTab(tabId)
    }
}
