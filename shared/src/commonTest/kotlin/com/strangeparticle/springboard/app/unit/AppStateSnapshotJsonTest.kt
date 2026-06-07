package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.luther.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [SpringboardAppSnapshot] — the compact-JSON serializer used to feed
 * app snapshots to the model.
 */
internal class SpringboardAppSnapshotJsonTest {

    private fun createViewModel() = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(),
        persistenceService = PersistenceServiceInMemoryFake(),
        platformActivationService = PlatformActivationServiceInMemoryFake(),
    )

    @Test
    fun `encode produces no whitespace and no newlines`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test.json")
        val snapshot = SpringboardAppSnapshot.capture(vm)

        val encoded = snapshot.toCompactJson()

        assertTrue(!encoded.contains("\n"), "Expected no newlines in compact form; got: $encoded")
        // Compact JSON has no spaces between separators (no `, ` after commas, no `: ` after colons),
        // but values like "Multi-Env With Common" or activator URLs may legitimately contain spaces.
        // Assert the structural separators are tight.
        assertTrue(!encoded.contains("\", \""),
            "Expected no `, ` between adjacent quoted tokens; got: $encoded")
        assertTrue(!encoded.contains("\": "),
            "Expected no `: ` between key and value; got: $encoded")
    }

    @Test
    fun `decode accepts compact form`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/test.json")
        val snapshot = SpringboardAppSnapshot.capture(vm)
        val compact = snapshot.toCompactJson()

        val decoded = SpringboardAppSnapshot.fromJson(compact)

        assertEquals(snapshot, decoded)
    }

    @Test
    fun `decode accepts pretty-printed form`() {
        // Hand-built pretty fixture — should parse identically.
        val pretty = """
            {
              "tabs": [
                {
                  "tabId": "tab-1",
                  "label": "Demo",
                  "source": "/foo.json",
                  "isDirty": false,
                  "springboard": null
                }
              ],
              "activeTabId": "tab-1"
            }
        """.trimIndent()

        val decoded = SpringboardAppSnapshot.fromJson(pretty)

        assertEquals(1, decoded.tabs.size)
        assertEquals("tab-1", decoded.tabs[0].tabId)
        assertEquals("Demo", decoded.tabs[0].label)
        assertEquals("/foo.json", decoded.tabs[0].source)
        assertEquals(false, decoded.tabs[0].isDirty)
    }

    @Test
    fun `roundTrip via encode-decode-encode is bitwise stable`() {
        val vm = createViewModel()
        vm.loadConfig(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test.json")
        val snapshot = SpringboardAppSnapshot.capture(vm)

        val first = snapshot.toCompactJson()
        val decoded = SpringboardAppSnapshot.fromJson(first)
        val second = decoded.toCompactJson()

        assertEquals(first, second)
    }
}
