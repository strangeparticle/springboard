package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.persistence.TabDto
import com.strangeparticle.springboard.app.persistence.TabsDto
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoader
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.viewmodel.TabRestorer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabRestorerTest {

    private fun jsonWithName(name: String): String = """
    {
      "name": "$name",
      "environments": [{ "id": "prod", "name": "Prod" }],
      "apps": [{ "id": "app1", "name": "App One" }],
      "resources": [{ "id": "res1", "name": "Res One" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com" }
      ]
    }
    """.trimIndent()

    private class RecordingLoader(
        private val contentsBySource: Map<String, String>,
    ) : SpringboardContentLoader {
        val calls = mutableListOf<String>()
        override suspend fun loadContent(source: String): String {
            calls += source
            return contentsBySource[source] ?: throw IllegalStateException("missing source: $source")
        }
    }

    private fun createViewModel(persistence: PersistenceServiceInMemoryFake): SpringboardViewModel {
        val settingsManager = SettingsManager(RuntimeEnvironment.Test, persistence).also { it.loadSettingsAtStartup() }
        return SpringboardViewModel(settingsManager, persistence)
    }

    @Test
    fun allTabsRestoredInOrderWithActiveMatchingDto() = runTest {
        val persistence = PersistenceServiceInMemoryFake()
        persistence.persistTabs(
            TabsDto(
                tabs = listOf(
                    TabDto(tabId = "persisted-1", source = "/a.json", zoomPercent = 125),
                    TabDto(tabId = "persisted-2", source = "/b.json", zoomPercent = 100),
                ),
                activeTabId = "persisted-2",
            )
        )
        val loader = RecordingLoader(
            mapOf("/a.json" to jsonWithName("A"), "/b.json" to jsonWithName("B"))
        )
        val errors = mutableListOf<String>()
        val restorer = TabRestorer(persistence, loader) { errors += it }
        val viewModel = createViewModel(persistence)

        restorer.restoreInto(viewModel)

        assertEquals(2, viewModel.tabs.size)
        assertEquals(listOf("A", "B"), viewModel.tabs.map { it.springboard?.name })
        assertEquals("B", viewModel.tabs.first { it.tabId == viewModel.activeTabId }.springboard?.name)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun missingEntryIsSkippedAndErrorReported() = runTest {
        val persistence = PersistenceServiceInMemoryFake()
        persistence.persistTabs(
            TabsDto(
                tabs = listOf(
                    TabDto(tabId = "p-1", source = "/a.json"),
                    TabDto(tabId = "p-2", source = "/missing.json"),
                    TabDto(tabId = "p-3", source = "/c.json"),
                ),
                activeTabId = "p-3",
            )
        )
        val loader = RecordingLoader(
            mapOf("/a.json" to jsonWithName("A"), "/c.json" to jsonWithName("C"))
        )
        val errors = mutableListOf<String>()
        val restorer = TabRestorer(persistence, loader) { errors += it }
        val viewModel = createViewModel(persistence)

        restorer.restoreInto(viewModel)

        assertEquals(listOf("A", "C"), viewModel.tabs.map { it.springboard?.name })
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("/missing.json"))
    }

    @Test
    fun noPersistedDtoLeavesInitialEmptyTab() = runTest {
        val persistence = PersistenceServiceInMemoryFake()
        val loader = RecordingLoader(emptyMap())
        val errors = mutableListOf<String>()
        val restorer = TabRestorer(persistence, loader) { errors += it }
        val viewModel = createViewModel(persistence)

        restorer.restoreInto(viewModel)

        assertEquals(1, viewModel.tabs.size)
        assertTrue(viewModel.tabs.first().isEmpty)
        assertTrue(errors.isEmpty())
        assertTrue(loader.calls.isEmpty())
    }

    @Test
    fun dtoWithEmptyListLeavesInitialEmptyTab() = runTest {
        val persistence = PersistenceServiceInMemoryFake()
        persistence.persistTabs(TabsDto(tabs = emptyList(), activeTabId = null))
        val loader = RecordingLoader(emptyMap())
        val errors = mutableListOf<String>()
        val restorer = TabRestorer(persistence, loader) { errors += it }
        val viewModel = createViewModel(persistence)

        restorer.restoreInto(viewModel)

        assertEquals(1, viewModel.tabs.size)
        assertTrue(viewModel.tabs.first().isEmpty)
        assertTrue(errors.isEmpty())
    }
}
