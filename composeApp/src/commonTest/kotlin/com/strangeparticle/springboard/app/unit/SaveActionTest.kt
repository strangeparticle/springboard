package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.factory.SpringboardJsonWriter
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SaveResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for SpringboardViewModel.saveActiveTab() — the per-source-type behavior matrix
 * from spec §2.4: local file → Save enabled; HTTP/HTTPS → NotSupported; null source
 * (empty tab) → NoSpringboard; write failure → WriteFailed; success → dirty cleared.
 */
class SaveActionTest {

    private fun createViewModel(fileService: PlatformFileContentServiceInMemoryFake) = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(),
        persistenceService = PersistenceServiceInMemoryFake(),
        platformActivationService = PlatformActivationServiceInMemoryFake(),
        fileContentService = fileService,
    )

    @Test
    fun `saveActiveTab on a local-file-sourced tab writes serialized output and clears dirty`() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/to/local.json")
        vm.markActiveTabDirty()

        val result = vm.saveActiveTab()

        assertEquals(SaveResult.Success("/path/to/local.json"), result)
        assertEquals(false, vm.activeTab?.isDirty)

        // What was written matches a fresh re-serialization of the loaded springboard.
        val springboard = vm.springboard
        assertNotNull(springboard)
        val expected = SpringboardJsonWriter.toJson(springboard)
        assertEquals(expected, fileService.writtenFiles["/path/to/local.json"])
    }

    @Test
    fun `saveActiveTab returns WriteFailed when fileService returns false`() {
        val fileService = PlatformFileContentServiceInMemoryFake().apply { writeReturnsOverride = false }
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/to/local.json")
        vm.markActiveTabDirty()

        val result = vm.saveActiveTab()

        assertTrue(result is SaveResult.WriteFailed)
        assertEquals("/path/to/local.json", (result as SaveResult.WriteFailed).path)
        // Dirty stays set on failure so the user can retry without losing the marker.
        assertEquals(true, vm.activeTab?.isDirty)
    }

    @Test
    fun `saveActiveTab returns WriteFailed when fileService throws`() {
        val fileService = PlatformFileContentServiceInMemoryFake().apply {
            writeException = RuntimeException("disk full")
        }
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/to/local.json")

        val result = vm.saveActiveTab()

        assertTrue(result is SaveResult.WriteFailed)
        assertEquals("disk full", (result as SaveResult.WriteFailed).errorMessage)
    }

    @Test
    fun `saveActiveTab returns NotSupportedForSource for an http-sourced tab`() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "https://example.com/sb.json")

        val result = vm.saveActiveTab()

        assertEquals(SaveResult.NotSupportedForSource, result)
        assertTrue(fileService.writtenFiles.isEmpty(), "Should not have attempted any writes")
    }

    @Test
    fun `saveActiveTab returns NoSpringboard when active tab is empty`() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService)

        val result = vm.saveActiveTab()

        assertEquals(SaveResult.NoSpringboard, result)
    }

    @Test
    fun `canSaveActiveTabInPlace is true for a loaded local-file tab`() {
        val vm = createViewModel(PlatformFileContentServiceInMemoryFake())
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/to/local.json")

        assertTrue(vm.canSaveActiveTabInPlace)
    }

    @Test
    fun `canSaveActiveTabInPlace is false for HTTP and empty tabs`() {
        val vm = createViewModel(PlatformFileContentServiceInMemoryFake())
        // Empty
        assertFalse(vm.canSaveActiveTabInPlace)

        // HTTP
        vm.loadConfig(TestFixtureJson.URL_ONLY, "https://example.com/sb.json")
        assertFalse(vm.canSaveActiveTabInPlace)
    }

    // ---- Save As (Task 6) ----

    @Test
    fun `saveActiveTabAs writes serialized output and rewrites tab source to the target path`() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "https://example.com/sb.json")
        vm.markActiveTabDirty()

        val result = vm.saveActiveTabAs("/local/path/copy.json")

        assertEquals(SaveResult.Success("/local/path/copy.json"), result)
        assertEquals("/local/path/copy.json", vm.activeTab?.source)
        assertEquals(false, vm.activeTab?.isDirty)
        assertTrue(fileService.writtenFiles.containsKey("/local/path/copy.json"))
    }

    @Test
    fun `saveActiveTabAs from a local-file source rewrites source to the new local path`() {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/to/original.json")

        val result = vm.saveActiveTabAs("/path/to/new-copy.json")

        assertEquals(SaveResult.Success("/path/to/new-copy.json"), result)
        assertEquals("/path/to/new-copy.json", vm.activeTab?.source)
    }

    @Test
    fun `saveActiveTabAs returns NoSpringboard for an empty tab`() {
        val vm = createViewModel(PlatformFileContentServiceInMemoryFake())
        val result = vm.saveActiveTabAs("/some/where.json")
        assertEquals(SaveResult.NoSpringboard, result)
    }

    @Test
    fun `saveActiveTabAs surfaces WriteFailed and does not rewrite tab source on failure`() {
        val fileService = PlatformFileContentServiceInMemoryFake().apply { writeReturnsOverride = false }
        val vm = createViewModel(fileService)
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/path/original.json")

        val result = vm.saveActiveTabAs("/path/new.json")

        assertTrue(result is SaveResult.WriteFailed)
        // tab.source must NOT change when the write fails — user can retry against the
        // same tab without leaving it pointing at a path that was never written.
        assertEquals("/path/original.json", vm.activeTab?.source)
    }
}
