package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.editio.toolcall.AddCommandActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddCommandActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddUrlActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddUrlActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddUrlTemplateActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddUrlTemplateActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.UpdateActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.UpdateActivatorToolCallHandler
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.UrlActivator
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ActivatorToolsTest {

    private fun loadedContext(json: String = TestFixtureJson.MULTI_ENV_WITH_COMMON): Pair<SpringboardToolCallExecutionContextInMemoryFake, String> {
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        )
        vm.loadConfig(json, "/test.json")
        return SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm) to vm.activeTabId
    }

    // ── add_url_activator ────────────────────────────────────────────────

    @Test
    fun `add_url_activator adds at unused coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (prod, app2, res2) is unused in MULTI_ENV_WITH_COMMON.
        val result = AddUrlActivatorToolCallHandler().executeToolCallHandler(
            AddUrlActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app2", resource_id = "res2", environment_id = "prod",
                url = "https://prod.example.com/app2/res2",
                display_message = "added",
            ),
            ctx,
        )

        assertTrue(result.success)
        val coordinate = Coordinate("prod", "app2", "res2")
        val activator = ctx.viewModel.springboardUnfiltered!!.indexes.activatorByCoordinate[coordinate]
        assertNotNull(activator)
        assertTrue(activator is UrlActivator)
        assertEquals("https://prod.example.com/app2/res2", activator.url)
        assertTrue(ctx.viewModel.activeTab!!.isDirty)
        assertEquals(1, ctx.stateChangedCount)
    }

    @Test
    fun `add_url_activator rejects duplicate coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (common, app1, res1) already exists in MULTI_ENV_WITH_COMMON.
        val result = AddUrlActivatorToolCallHandler().executeToolCallHandler(
            AddUrlActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                url = "https://x.example.com",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("duplicate_coordinate", result.code)
    }

    @Test
    fun `add_url_activator rejects missing app reference`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddUrlActivatorToolCallHandler().executeToolCallHandler(
            AddUrlActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "no_such_app", resource_id = "res1", environment_id = "prod",
                url = "https://x.example.com",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_reference", result.code)
    }

    @Test
    fun `add_url_activator accepts ALL as the environment id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddUrlActivatorToolCallHandler().executeToolCallHandler(
            AddUrlActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "ALL",
                url = "https://everywhere.example.com",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        assertNotNull(ctx.viewModel.springboardUnfiltered!!.indexes.activatorByCoordinate[Coordinate("ALL", "app1", "res1")])
    }

    // ── add_command_activator and add_url_template_activator (smoke) ─────

    @Test
    fun `add_command_activator adds with command_template`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (preprod, app2, res1) is unused — pick it to avoid coordinate collisions.
        val result = AddCommandActivatorToolCallHandler().executeToolCallHandler(
            AddCommandActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app2", resource_id = "res1", environment_id = "preprod",
                command_template = "echo hello",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
    }

    @Test
    fun `add_url_template_activator adds with url_template`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (preprod, app2, res2) is unused.
        val result = AddUrlTemplateActivatorToolCallHandler().executeToolCallHandler(
            AddUrlTemplateActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app2", resource_id = "res2", environment_id = "preprod",
                url_template = "https://{env}.example.com",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
    }

    // ── update_activator ────────────────────────────────────────────────

    @Test
    fun `update_activator changes url for a URL activator`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateActivatorToolCallHandler().executeToolCallHandler(
            UpdateActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                url = "https://updated.example.com",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        val activator = ctx.viewModel.springboardUnfiltered!!.indexes.activatorByCoordinate[Coordinate("common", "app1", "res1")]
        assertTrue(activator is UrlActivator)
        assertEquals("https://updated.example.com", activator.url)
    }

    @Test
    fun `update_activator rejects payload that does not match the existing type`() = runTest {
        val (ctx, tabId) = loadedContext()

        // (common, app1, res1) is a URL activator — passing command_template should fail.
        val result = UpdateActivatorToolCallHandler().executeToolCallHandler(
            UpdateActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                command_template = "echo wrong",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("wrong_field_for_type", result.code)
    }

    @Test
    fun `update_activator rejects missing target`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateActivatorToolCallHandler().executeToolCallHandler(
            UpdateActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "no_such_env",
                url = "x",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_target", result.code)
    }

    // ── remove_activator ────────────────────────────────────────────────

    @Test
    fun `remove_activator deletes the activator at the coordinate`() = runTest {
        val (ctx, tabId) = loadedContext()
        val coord = Coordinate("common", "app1", "res1")
        assertNotNull(ctx.viewModel.springboardUnfiltered!!.indexes.activatorByCoordinate[coord])

        val result = RemoveActivatorToolCallHandler().executeToolCallHandler(
            RemoveActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "app1", resource_id = "res1", environment_id = "common",
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        assertNull(ctx.viewModel.springboardUnfiltered!!.indexes.activatorByCoordinate[coord])
    }

    @Test
    fun `remove_activator rejects when guidance still references the coordinate`() = runTest {
        val withGuidance = """
        {
          "name": "guided",
          "appGroups": [],
          "apps": [{"id":"a1","name":"A1"}],
          "resources": [{"id":"r1","name":"R1"}],
          "environments": [{"id":"e1","name":"E1"}],
          "activators": [
            {"type":"url","appId":"a1","resourceId":"r1","environmentId":"e1","url":"https://x.example.com"}
          ],
          "guidanceData": [
            {"environmentId":"e1","appId":"a1","resourceId":"r1","guidanceLines":["a tip"]}
          ]
        }
        """.trimIndent()
        val (ctx, tabId) = loadedContext(withGuidance)

        val result = RemoveActivatorToolCallHandler().executeToolCallHandler(
            RemoveActivatorToolCallHandlerRequest(
                tab_id = tabId, app_id = "a1", resource_id = "r1", environment_id = "e1",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("in_use", result.code)
    }
}
