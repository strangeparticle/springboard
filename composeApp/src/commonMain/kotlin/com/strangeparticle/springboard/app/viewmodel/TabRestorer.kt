package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class TabRestorer(
    private val persistenceService: PersistenceService,
    private val loader: SpringboardContentLoader,
    private val reportError: (String) -> Unit = { ToastBroadcaster.error(it) },
) {

    /**
     * Restores tabs from the resolved [tabSources] list (already resolved by the
     * settings precedence chain). Zoom levels are read from the separate tab
     * persistence layer when the source matches.
     */
    suspend fun restoreInto(viewModel: SpringboardViewModel, tabSources: List<String>) {
        if (tabSources.isEmpty()) return

        val persistedTabs = persistenceService.loadTabs()
        val zoomBySource = persistedTabs?.tabs
            ?.filter { it.source != null }
            ?.associate { it.source!! to (it.zoomPercent ?: 100) }
            ?: emptyMap()

        viewModel.runSuppressingAutosave {
            for (source in tabSources) {
                if (!viewModel.canCreateNewTab && !isReusableFirstEmptyTab(viewModel)) {
                    reportError("Skipped restoring tab for '$source': maximum of $MAX_OPEN_TABS tabs reached")
                    continue
                }
                val jsonContents = try {
                    loader.loadContent(source)
                } catch (e: Exception) {
                    reportError("Failed to restore tab for '$source': ${e.message}")
                    continue
                }
                val zoomPercent = zoomBySource[source] ?: 100
                viewModel.restoreTabFromPersistence(
                    source = source,
                    jsonContents = jsonContents,
                    zoomPercent = zoomPercent,
                )
            }
        }
    }

    private fun isReusableFirstEmptyTab(viewModel: SpringboardViewModel): Boolean {
        return viewModel.tabs.size == 1 && viewModel.tabs.first().isEmpty
    }
}
