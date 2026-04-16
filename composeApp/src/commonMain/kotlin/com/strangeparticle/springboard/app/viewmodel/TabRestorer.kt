package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class TabRestorer(
    private val persistenceService: PersistenceService,
    private val loader: SpringboardContentLoader,
    private val reportError: (String) -> Unit = { ToastBroadcaster.error(it) },
) {

    suspend fun restoreInto(viewModel: SpringboardViewModel) {
        val persisted = persistenceService.loadTabs() ?: return
        val entriesWithSource = persisted.tabs.filter { it.source != null }
        if (entriesWithSource.isEmpty()) return

        val persistedToInMemoryId = mutableMapOf<String, String>()

        viewModel.runSuppressingAutosave {
            for (entry in entriesWithSource) {
                val source = entry.source ?: continue
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
                val newTabId = viewModel.restoreTabFromPersistence(
                    source = source,
                    jsonContents = jsonContents,
                    zoomPercent = entry.zoomPercent ?: 100,
                )
                persistedToInMemoryId[entry.tabId] = newTabId
            }

            val desiredPersistedActive = persisted.activeTabId
            val desiredInMemoryActive = desiredPersistedActive?.let { persistedToInMemoryId[it] }
                ?: persistedToInMemoryId.values.firstOrNull()
            if (desiredInMemoryActive != null) {
                viewModel.selectTab(desiredInMemoryActive)
            }
        }
    }

    private fun isReusableFirstEmptyTab(viewModel: SpringboardViewModel): Boolean {
        return viewModel.tabs.size == 1 && viewModel.tabs.first().isEmpty
    }
}
