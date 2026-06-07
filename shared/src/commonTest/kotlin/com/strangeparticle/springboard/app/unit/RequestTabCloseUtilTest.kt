package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.tabs.requestTabClose
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [requestTabClose] — the close-decision helper extracted from MainScreen's
 * `onClose` callback.
 *
 * Rule: dirty tab → invoke `queueConfirmationDialog(tabId)` (caller renders the dialog);
 * clean tab → `viewModel.closeTab(tabId)` immediately.
 */
class RequestTabCloseUtilTest {

    private fun createViewModel() = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(),
        persistenceService = PersistenceServiceInMemoryFake(),
        platformActivationService = PlatformActivationServiceInMemoryFake(),
    )

    @Test
    fun `requestTabClose closes a clean tab immediately and does not queue confirmation`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/clean.json")
        val cleanTabId = vm.activeTabId

        var queuedTabId: String? = null
        requestTabClose(cleanTabId, vm) { queuedTabId = it }

        assertTrue(vm.tabs.none { it.tabId == cleanTabId }, "Clean tab should be closed immediately")
        assertNull(queuedTabId, "Confirmation should not have been queued for a clean tab")
    }

    @Test
    fun `requestTabClose on a dirty tab queues confirmation and does not close`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/dirty.json")
        vm.markActiveTabDirty()
        val dirtyTabId = vm.activeTabId

        var queuedTabId: String? = null
        requestTabClose(dirtyTabId, vm) { queuedTabId = it }

        assertEquals(dirtyTabId, queuedTabId, "Confirmation should have been queued for the dirty tab")
        assertTrue(vm.tabs.any { it.tabId == dirtyTabId }, "Dirty tab should NOT be closed yet")
    }

}
