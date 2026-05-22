package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.editio.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.domain.factory.dto.CommandActivatorDto
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [SpringboardAppSnapshot.capture] — the per-turn snapshot the AI session
 * manager sends back to the model so it sees the exact tab/state landscape after
 * any mutation.
 */
internal class SpringboardAppSnapshotTest {

    private fun createViewModel(target: RuntimeEnvironment = RuntimeEnvironment.Test) = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(target = target),
        persistenceService = PersistenceServiceInMemoryFake(),
        platformActivationService = PlatformActivationServiceInMemoryFake(),
    )

    @Test
    fun `capture includes every open tab`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/tab1.json")
        vm.createTab()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/tab2.json")
        vm.createTab() // empty tab waiting for an open

        val snapshot = SpringboardAppSnapshot.capture(vm)

        assertEquals(3, snapshot.tabs.size)
        assertEquals(listOf("/tab1.json", "/tab2.json", null), snapshot.tabs.map { it.source })
    }

    @Test
    fun `capture sets activeTabId to the currently active tab`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/tab1.json")
        val firstTabId = vm.activeTabId
        vm.createTab()
        val secondTabId = vm.activeTabId
        assertTrue(firstTabId != secondTabId)

        val snapshot = SpringboardAppSnapshot.capture(vm)

        assertEquals(secondTabId, snapshot.activeTabId)
    }

    @Test
    fun `capture sets springboard to null for an empty tab`() {
        val vm = createViewModel() // viewmodel constructor seeds one empty tab

        val snapshot = SpringboardAppSnapshot.capture(vm)

        assertEquals(1, snapshot.tabs.size)
        assertNull(snapshot.tabs[0].springboard)
        assertNull(snapshot.tabs[0].source)
    }

    @Test
    fun `capture maps a loaded springboard to its DTO form`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/tab1.json")

        val snapshot = SpringboardAppSnapshot.capture(vm)
        val dto = snapshot.tabs.first().springboard

        assertNotNull(dto)
        assertEquals("Multi-Env With Common", dto.name)
        assertEquals(listOf("common", "preprod", "prod"), dto.environments.map { it.id })
        assertEquals(listOf("app1", "app2"), dto.apps.map { it.id })
    }

    @Test
    fun `capture propagates per-tab dirty flag`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/tab1.json")
        vm.markActiveTabDirty()

        val snapshot = SpringboardAppSnapshot.capture(vm)

        assertEquals(true, snapshot.tabs.first().isDirty)
    }

    @Test
    fun `capture uses unfiltered springboard on wasm`() {
        val vm = createViewModel(target = RuntimeEnvironment.WASM)
        vm.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/tab1.json")

        val snapshot = SpringboardAppSnapshot.capture(vm)

        val dto = snapshot.tabs.first().springboard
        assertNotNull(dto)
        assertEquals(1, dto.activators.filterIsInstance<CommandActivatorDto>().size)
    }
}
