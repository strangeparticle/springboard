package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.editio.toolcall.AddGuidanceToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddGuidanceToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveGuidanceToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveGuidanceToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.UpdateGuidanceToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.UpdateGuidanceToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GuidanceToolsTest {

    private fun loadedContext(json: String = TestFixtureJson.MULTI_ENV_WITH_COMMON): Pair<SpringboardToolCallExecutionContextInMemoryFake, String> {
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        )
        vm.loadConfig(json, "/test.json")
        return SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm) to vm.activeTabId
    }

    @Test
    fun `add_guidance succeeds when an activator exists at the coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (common, app1, res1) has an activator in MULTI_ENV_WITH_COMMON.
        val result = AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("Tip: open dashboards in production."),
                display_message = "added",
            ),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(1, ctx.viewModel.springboard!!.guidanceData.size)
    }

    @Test
    fun `add_guidance refuses when no activator exists at the coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (prod, app2, res2) has no activator.
        val result = AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app2", resource_id = "res2", environment_id = "prod",
                guidance_lines = listOf("won't apply"),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_activator", result.code)
    }

    @Test
    fun `add_guidance refuses duplicate at same coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()

        AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("first"),
                display_message = "x",
            ),
            ctx,
        )
        val second = AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("second"),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(second.success)
        assertEquals("duplicate_coordinate", second.code)
    }

    @Test
    fun `update_guidance replaces the guidance lines`() = runTest {
        val (ctx, tabId) = loadedContext()
        AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("old"),
                display_message = "x",
            ),
            ctx,
        )

        val result = UpdateGuidanceToolCallHandler().executeToolCallHandler(
            UpdateGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("new line 1", "new line 2"),
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        val guidance = ctx.viewModel.springboard!!.guidanceData.first()
        assertEquals(listOf("new line 1", "new line 2"), guidance.guidanceLines)
    }

    @Test
    fun `update_guidance rejects missing target`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateGuidanceToolCallHandler().executeToolCallHandler(
            UpdateGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("x"),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_target", result.code)
    }

    @Test
    fun `remove_guidance deletes the entry`() = runTest {
        val (ctx, tabId) = loadedContext()
        AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("hello"),
                display_message = "x",
            ),
            ctx,
        )

        val result = RemoveGuidanceToolCallHandler().executeToolCallHandler(
            RemoveGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(0, ctx.viewModel.springboard!!.guidanceData.size)
    }

    @Test
    fun `add_guidance rejects empty guidance_lines`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = emptyList(),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("empty_guidance_lines", result.code)
        assertEquals(0, ctx.viewModel.springboard!!.guidanceData.size)
    }

    @Test
    fun `update_guidance rejects empty guidance_lines`() = runTest {
        val (ctx, tabId) = loadedContext()
        AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("original"),
                display_message = "x",
            ),
            ctx,
        )

        val result = UpdateGuidanceToolCallHandler().executeToolCallHandler(
            UpdateGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = emptyList(),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("empty_guidance_lines", result.code)
        assertEquals(listOf("original"), ctx.viewModel.springboard!!.guidanceData.first().guidanceLines)
    }

    @Test
    fun `add_guidance rejects blank guidance lines`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("valid", "  "),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("empty_guidance_line", result.code)
        assertEquals(0, ctx.viewModel.springboard!!.guidanceData.size)
    }

    @Test
    fun `update_guidance rejects blank guidance lines`() = runTest {
        val (ctx, tabId) = loadedContext()
        AddGuidanceToolCallHandler().executeToolCallHandler(
            AddGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("original"),
                display_message = "x",
            ),
            ctx,
        )

        val result = UpdateGuidanceToolCallHandler().executeToolCallHandler(
            UpdateGuidanceToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                guidance_lines = listOf("new", ""),
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("empty_guidance_line", result.code)
        assertEquals(listOf("original"), ctx.viewModel.springboard!!.guidanceData.first().guidanceLines)
    }
}
