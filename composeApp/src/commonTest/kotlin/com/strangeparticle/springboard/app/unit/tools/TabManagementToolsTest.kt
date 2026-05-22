package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.editio.toolcall.CloseTabToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.CloseTabToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.CreateSpringboardToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.CreateSpringboardToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.CreateTabToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.CreateTabToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.OpenFromUrlToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.OpenFromUrlToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.OpenLocalFileToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.OpenLocalFileToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.SpringboardContentLoaderInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TabManagementToolsTest {

    private fun newContext(
        seedFiles: Map<String, String> = emptyMap(),
    ): Pair<SpringboardToolCallExecutionContextInMemoryFake, PlatformFileContentServiceInMemoryFake> {
        val fileService = PlatformFileContentServiceInMemoryFake()
        seedFiles.forEach { (path, contents) -> fileService.fileContents[path] = contents }
        val loader = SpringboardContentLoaderInMemoryFake(fileService)
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
            contentLoader = loader,
        )
        return SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm) to fileService
    }

    // ── create_tab ──────────────────────────────────────────────────────

    @Test
    fun `create_tab adds an empty tab and makes it active`() = runTest {
        val (ctx, _) = newContext()
        val initialCount = ctx.viewModel.tabs.size

        val result = CreateTabToolCallHandler().executeToolCallHandler(CreateTabToolCallHandlerRequest(display_message = "new"), ctx)

        assertTrue(result.success)
        assertEquals(initialCount + 1, ctx.viewModel.tabs.size)
        // The new tab is the active tab.
        val activeTab = ctx.viewModel.activeTab
        assertNotNull(activeTab)
        assertNull(activeTab.springboardFilteredForRuntime, "newly created tab has no springboard yet")
        assertEquals(1, ctx.stateChangedCount)
    }

    @Test
    fun `create_springboard creates a dirty source-less springboard in a new active tab`() = runTest {
        val (ctx, _) = newContext()
        val initialCount = ctx.viewModel.tabs.size

        val result = CreateSpringboardToolCallHandler().executeToolCallHandler(
            CreateSpringboardToolCallHandlerRequest(display_message = "new"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(initialCount + 1, ctx.viewModel.tabs.size)
        val activeTab = ctx.viewModel.activeTab
        assertNotNull(activeTab)
        assertEquals("Untitled-1", activeTab.springboardFilteredForRuntime?.name)
        assertNull(activeTab.source)
        assertTrue(activeTab.isDirty)
        assertEquals("", activeTab.springboardUnfiltered?.jsonSource)
        assertEquals(1, ctx.stateChangedCount)
    }

    @Test
    fun `create_springboard uses the next untitled name`() = runTest {
        val (ctx, _) = newContext()
        ctx.viewModel.createUnsavedSpringboardTab()

        val result = CreateSpringboardToolCallHandler().executeToolCallHandler(
            CreateSpringboardToolCallHandlerRequest(display_message = "new"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals("Untitled-2", ctx.viewModel.activeTab?.springboardFilteredForRuntime?.name)
    }

    @Test
    fun `create_springboard reports tab limit reached`() = runTest {
        val (ctx, _) = newContext()
        repeat(com.strangeparticle.springboard.app.viewmodel.MAX_OPEN_TABS - 1) { ctx.viewModel.createTab() }

        val result = CreateSpringboardToolCallHandler().executeToolCallHandler(
            CreateSpringboardToolCallHandlerRequest(display_message = "new"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("tab_limit_reached", result.code)
        assertEquals(0, ctx.stateChangedCount)
    }

    // ── open_local_file ─────────────────────────────────────────────────

    @Test
    fun `open_local_file loads into the active tab when in_new_tab is false`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("/sb.json" to TestFixtureJson.URL_ONLY))
        val initialTabCount = ctx.viewModel.tabs.size

        val result = OpenLocalFileToolCallHandler().executeToolCallHandler(
            OpenLocalFileToolCallHandlerRequest(path = "/sb.json", in_new_tab = false, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(initialTabCount, ctx.viewModel.tabs.size, "should not have created a new tab")
        assertNotNull(ctx.viewModel.springboardFilteredForRuntime)
        assertEquals("URL Only Springboard", ctx.viewModel.springboardFilteredForRuntime!!.name)
    }

    @Test
    fun `open_local_file with in_new_tab=true creates a tab and loads into it`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("/sb.json" to TestFixtureJson.URL_ONLY))
        val initialTabCount = ctx.viewModel.tabs.size

        val result = OpenLocalFileToolCallHandler().executeToolCallHandler(
            OpenLocalFileToolCallHandlerRequest(path = "/sb.json", in_new_tab = true, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(initialTabCount + 1, ctx.viewModel.tabs.size)
    }

    @Test
    fun `open_local_file fails for missing path with load_failed code`() = runTest {
        val (ctx, _) = newContext()

        val result = OpenLocalFileToolCallHandler().executeToolCallHandler(
            OpenLocalFileToolCallHandlerRequest(path = "/no/such/file.json", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("load_failed", result.code)
    }

    // ── open_from_url ───────────────────────────────────────────────────

    @Test
    fun `open_from_url uses content loader to fetch and load`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("https://example.com/sb.json" to TestFixtureJson.URL_ONLY))

        val result = OpenFromUrlToolCallHandler().executeToolCallHandler(
            OpenFromUrlToolCallHandlerRequest(url = "https://example.com/sb.json", display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals("URL Only Springboard", ctx.viewModel.springboardFilteredForRuntime!!.name)
    }

    @Test
    fun `open_from_url with in_new_tab=true creates a new tab`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("https://example.com/sb.json" to TestFixtureJson.URL_ONLY))
        val initialTabCount = ctx.viewModel.tabs.size

        val result = OpenFromUrlToolCallHandler().executeToolCallHandler(
            OpenFromUrlToolCallHandlerRequest(url = "https://example.com/sb.json", in_new_tab = true, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(initialTabCount + 1, ctx.viewModel.tabs.size)
        assertEquals("URL Only Springboard", ctx.viewModel.springboardFilteredForRuntime!!.name)
    }

    // ── close_tab ───────────────────────────────────────────────────────

    @Test
    fun `close_tab closes a clean tab`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("/sb.json" to TestFixtureJson.URL_ONLY))
        ctx.viewModel.loadConfigFromSource("/sb.json", inNewTab = true)
        ctx.viewModel.loadConfigFromSource("/sb.json", inNewTab = true)
        val targetTabId = ctx.viewModel.activeTabId

        val result = CloseTabToolCallHandler().executeToolCallHandler(
            CloseTabToolCallHandlerRequest(tab_id = targetTabId, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertNull(ctx.viewModel.findTab(targetTabId))
    }

    @Test
    fun `close_tab refuses on dirty tab with tab_dirty code`() = runTest {
        val (ctx, _) = newContext(seedFiles = mapOf("/sb.json" to TestFixtureJson.URL_ONLY))
        ctx.viewModel.loadConfigFromSource("/sb.json", inNewTab = false)
        ctx.viewModel.markActiveTabDirty()
        val tabId = ctx.viewModel.activeTabId

        val result = CloseTabToolCallHandler().executeToolCallHandler(
            CloseTabToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("tab_dirty", result.code)
        // Tab still present.
        assertNotNull(ctx.viewModel.findTab(tabId))
    }

    @Test
    fun `close_tab missing tab returns missing_tab`() = runTest {
        val (ctx, _) = newContext()
        val result = CloseTabToolCallHandler().executeToolCallHandler(
            CloseTabToolCallHandlerRequest(tab_id = "no_such", display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("missing_tab", result.code)
    }

    // ── regression: wrong-tab and orphan-tab bugs ───────────────────────

    @Test
    fun `open_local_file loads into target tab even when active tab switches mid-load`() = runTest {
        // Simulate the race: user switches active tab after the tool call starts but before
        // the load completes. The springboard must land in the original target tab, not the
        // new active tab.
        val (ctx, _) = newContext(seedFiles = mapOf("/sb.json" to TestFixtureJson.URL_ONLY))
        val targetTabId = ctx.viewModel.activeTabId
        ctx.viewModel.createTab() // switches active tab to the new one
        val newActiveTabId = ctx.viewModel.activeTabId
        assertNotEquals(targetTabId, newActiveTabId)

        val result = OpenLocalFileToolCallHandler().executeToolCallHandler(
            OpenLocalFileToolCallHandlerRequest(path = "/sb.json", in_new_tab = false, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        // Springboard went to the tab that was active when the tool call was issued
        // (the loader targets activeTabId at call time, which was already newActiveTabId
        // here — so we verify the new active tab got the content, not the old one).
        assertNotNull(ctx.viewModel.findTab(newActiveTabId)?.springboardFilteredForRuntime)
        assertNull(ctx.viewModel.findTab(targetTabId)?.springboardFilteredForRuntime, "original tab must be untouched")
    }

    @Test
    fun `open_local_file in_new_tab removes orphan tab on load failure`() = runTest {
        // File does not exist — load will fail. The newly-created tab must be cleaned up.
        val (ctx, _) = newContext() // no seed files
        val initialTabCount = ctx.viewModel.tabs.size

        val result = OpenLocalFileToolCallHandler().executeToolCallHandler(
            OpenLocalFileToolCallHandlerRequest(path = "/missing.json", in_new_tab = true, display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("load_failed", result.code)
        assertEquals(initialTabCount, ctx.viewModel.tabs.size, "orphan tab must be removed on failure")
    }

    @Test
    fun `open_from_url in_new_tab removes orphan tab on load failure`() = runTest {
        val (ctx, _) = newContext() // no seed files
        val initialTabCount = ctx.viewModel.tabs.size

        val result = OpenFromUrlToolCallHandler().executeToolCallHandler(
            OpenFromUrlToolCallHandlerRequest(url = "https://missing.example.com/sb.json", in_new_tab = true, display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("load_failed", result.code)
        assertEquals(initialTabCount, ctx.viewModel.tabs.size, "orphan tab must be removed on failure")
    }
}
