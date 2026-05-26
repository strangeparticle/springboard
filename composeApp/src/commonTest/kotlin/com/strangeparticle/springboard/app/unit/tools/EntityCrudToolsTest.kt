package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.editio.toolcall.AddAppToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddAppGroupToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddAppGroupToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddAppToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddEnvironmentToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddEnvironmentToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddResourceToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.AddResourceToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.ChangeResourceIdToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.ChangeResourceIdToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.ChangeResourceNameToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.ChangeResourceNameToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveAppToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveAppGroupToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveAppGroupToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveAppToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveEnvironmentToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveEnvironmentToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveResourceToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.RemoveResourceToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.UpdateAppToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.UpdateAppGroupToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.UpdateAppGroupToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.UpdateAppToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.UpdateEnvironmentToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.UpdateEnvironmentToolCallHandler
import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.domain.mutator.addApp
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the per-entity CRUD tools (apps, resources, environments, app groups).
 * Per spec §4.2.
 *
 * Coverage shape per tool:
 * - happy path (mutation succeeds, dirty flag flips, markStateChanged called)
 * - missing-tab error
 * - relevant validation failure(s) (duplicate id / missing target / in-use / missing reference)
 *
 * The pure mutator logic is exercised indirectly through these tool tests; the
 * tools' wiring (deserialize → mutator → applyMutation) is what we're verifying.
 */
internal class EntityCrudToolsTest {

    private fun loadedContext(json: String = TestFixtureJson.MULTI_ENV_WITH_COMMON): Pair<SpringboardToolCallExecutionContextInMemoryFake, String> {
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        )
        vm.loadConfig(json, "/test.json")
        return SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm) to vm.activeTabId
    }

    // ── add_app ──────────────────────────────────────────────────────────

    @Test
    fun `addApp throws structured error for duplicate id`() {
        val (ctx, _) = loadedContext()

        val error = assertFailsWith<SpringboardMutationError> {
            addApp(ctx.viewModel.springboardUnfiltered!!, App(id = "app1", name = "Dup"))
        }

        assertEquals("duplicate_id", error.code)
        assertEquals("An app with id 'app1' already exists in this springboard.", error.errorMessage)
    }

    @Test
    fun `add_app appends to springboard, flips dirty, marks state changed`() = runTest {
        val (ctx, tabId) = loadedContext()
        val initialAppCount = ctx.viewModel.springboardUnfiltered!!.apps.size

        val result = AddAppToolCallHandler().executeToolCallHandler(
            AddAppToolCallHandlerRequest(tab_id = tabId, id = "auth", name = "Auth Service", display_message = "added"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(initialAppCount + 1, ctx.viewModel.springboardUnfiltered!!.apps.size)
        assertTrue(ctx.viewModel.activeTab!!.isDirty)
        assertEquals(1, ctx.stateChangedCount)
    }

    @Test
    fun `add_app rejects duplicate id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddAppToolCallHandler().executeToolCallHandler(
            AddAppToolCallHandlerRequest(tab_id = tabId, id = "app1", name = "Dup", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("duplicate_id", result.code)
        assertFalse(ctx.viewModel.activeTab!!.isDirty)
        assertEquals(0, ctx.stateChangedCount)
    }

    @Test
    fun `add_app rejects missing app_group_id reference`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddAppToolCallHandler().executeToolCallHandler(
            AddAppToolCallHandlerRequest(tab_id = tabId, id = "newApp", name = "X", app_group_id = "no_such_group", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_reference", result.code)
    }

    @Test
    fun `add_app on missing tab returns missing_tab error`() = runTest {
        val (ctx, _) = loadedContext()

        val result = AddAppToolCallHandler().executeToolCallHandler(
            AddAppToolCallHandlerRequest(tab_id = "no_such_tab", id = "x", name = "x", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_tab", result.code)
    }

    // ── update_app ──────────────────────────────────────────────────────

    @Test
    fun `update_app changes only the specified fields`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateAppToolCallHandler().executeToolCallHandler(
            UpdateAppToolCallHandlerRequest(tab_id = tabId, id = "app1", name = "Renamed App", display_message = "renamed"),
            ctx,
        )

        assertTrue(result.success)
        val updated = ctx.viewModel.springboardUnfiltered!!.apps.first { it.id == "app1" }
        assertEquals("Renamed App", updated.name)
        assertTrue(ctx.viewModel.activeTab!!.isDirty)
    }

    @Test
    fun `update_app rejects missing target`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateAppToolCallHandler().executeToolCallHandler(
            UpdateAppToolCallHandlerRequest(tab_id = tabId, id = "no_such_app", name = "x", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_target", result.code)
    }

    @Test
    fun `update_app clears app_group_id when clear_app_group_id is true`() = runTest {
        val groupedJson = """
        {
          "name": "grouped", "appGroups": [{"id":"g1","description":"G"}],
          "apps": [{"id":"a1","name":"A1","appGroupId":"g1"}],
          "resources": [{"id":"r1","name":"R1"}],
          "environments": [{"id":"e1","name":"E1"}],
          "activators": [{"type":"url","appId":"a1","resourceId":"r1","environmentId":"e1","url":"https://x.example.com"}]
        }
        """.trimIndent()
        val (ctx, tabId) = loadedContext(groupedJson)

        val result = UpdateAppToolCallHandler().executeToolCallHandler(
            UpdateAppToolCallHandlerRequest(tab_id = tabId, id = "a1", clear_app_group_id = true, display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertNull(ctx.viewModel.springboardUnfiltered!!.apps.first { it.id == "a1" }.appGroupId)
    }

    @Test
    fun `update_app sets app_group_id when provided`() = runTest {
        val groupedJson = """
        {
          "name": "grouped", "appGroups": [{"id":"g1","description":"G"}],
          "apps": [{"id":"a1","name":"A1"}],
          "resources": [{"id":"r1","name":"R1"}],
          "environments": [{"id":"e1","name":"E1"}],
          "activators": [{"type":"url","appId":"a1","resourceId":"r1","environmentId":"e1","url":"https://x.example.com"}]
        }
        """.trimIndent()
        val (ctx, tabId) = loadedContext(groupedJson)

        val result = UpdateAppToolCallHandler().executeToolCallHandler(
            UpdateAppToolCallHandlerRequest(tab_id = tabId, id = "a1", app_group_id = "g1", display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertEquals("g1", ctx.viewModel.springboardUnfiltered!!.apps.first { it.id == "a1" }.appGroupId)
    }

    @Test
    fun `update_app rejects conflicting app_group_id and clear_app_group_id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = UpdateAppToolCallHandler().executeToolCallHandler(
            UpdateAppToolCallHandlerRequest(tab_id = tabId, id = "app1", app_group_id = "g1", clear_app_group_id = true, display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("conflicting_fields", result.code)
        assertFalse(ctx.viewModel.activeTab!!.isDirty)
        assertEquals(0, ctx.stateChangedCount)
    }

    // ── blank id guards ─────────────────────────────────────────────────

    @Test
    fun `add_app rejects blank id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddAppToolCallHandler().executeToolCallHandler(
            AddAppToolCallHandlerRequest(tab_id = tabId, id = "  ", name = "Blank", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("blank_id", result.code)
    }

    @Test
    fun `add_resource rejects blank id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddResourceToolCallHandler().executeToolCallHandler(
            AddResourceToolCallHandlerRequest(tab_id = tabId, id = "", name = "Blank", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("blank_id", result.code)
    }

    @Test
    fun `add_environment rejects blank id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddEnvironmentToolCallHandler().executeToolCallHandler(
            AddEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "", name = "Blank", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("blank_id", result.code)
    }

    @Test
    fun `add_app_group rejects blank id`() = runTest {
        val (ctx, tabId) = loadedContext()

        val result = AddAppGroupToolCallHandler().executeToolCallHandler(
            AddAppGroupToolCallHandlerRequest(tab_id = tabId, id = "", description = "Blank", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("blank_id", result.code)
    }

    // ── remove_app ──────────────────────────────────────────────────────

    @Test
    fun `remove_app refuses when app has activators`() = runTest {
        val (ctx, tabId) = loadedContext()
        // app1 in MULTI_ENV_WITH_COMMON has activators referring to it.

        val result = RemoveAppToolCallHandler().executeToolCallHandler(
            RemoveAppToolCallHandlerRequest(tab_id = tabId, id = "app1", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("in_use", result.code)
    }

    @Test
    fun `remove_app succeeds for app with no activator references`() = runTest {
        // Fixture with two apps, one of which (app2) has no activators referring to it.
        val noRefJson = """
        {
          "name": "Two apps", "appGroups": [],
          "apps": [{"id":"a1","name":"A1"},{"id":"a2","name":"A2"}],
          "resources": [{"id":"r1","name":"R1"}],
          "environments": [{"id":"e1","name":"E1"}],
          "activators": [
            {"type":"url","appId":"a1","resourceId":"r1","environmentId":"e1","url":"https://x.example.com"}
          ]
        }
        """.trimIndent()
        val (ctx, tabId) = loadedContext(noRefJson)

        val result = RemoveAppToolCallHandler().executeToolCallHandler(
            RemoveAppToolCallHandlerRequest(tab_id = tabId, id = "a2", display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        assertTrue(ctx.viewModel.springboardUnfiltered!!.apps.none { it.id == "a2" })
    }

    // ── resource CRUD (lighter coverage — same pattern as app) ──────────

    @Test
    fun `add_resource success`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = AddResourceToolCallHandler().executeToolCallHandler(
            AddResourceToolCallHandlerRequest(tab_id = tabId, id = "newRes", name = "New Res", display_message = "x"),
            ctx,
        )
        assertTrue(result.success)
        assertTrue(ctx.viewModel.springboardUnfiltered!!.resources.any { it.id == "newRes" })
    }

    @Test
    fun `change_resource_name changes only resource name`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ChangeResourceNameToolCallHandler().executeToolCallHandler(
            ChangeResourceNameToolCallHandlerRequest(tab_id = tabId, id = "res1", name = "Renamed Resource", display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        val springboard = ctx.viewModel.springboardUnfiltered!!
        assertTrue(springboard.resources.any { it.id == "res1" && it.name == "Renamed Resource" })
        assertTrue(springboard.activators.any { it.resourceId == "res1" })
    }

    @Test
    fun `change_resource_id rewrites resource id throughout springboard tree`() = runTest {
        val (ctx, tabId) = loadedContext(TestFixtureJson.MULTI_ENV_WITH_GUIDANCE)
        val result = ChangeResourceIdToolCallHandler().executeToolCallHandler(
            ChangeResourceIdToolCallHandlerRequest(tab_id = tabId, id = "res1", new_id = "trd", display_message = "x"),
            ctx,
        )

        assertTrue(result.success)
        val springboard = ctx.viewModel.springboardUnfiltered!!
        assertTrue(springboard.resources.any { it.id == "trd" && it.name == "Dashboard" })
        assertTrue(springboard.resources.none { it.id == "res1" })
        assertTrue(springboard.activators.none { it.resourceId == "res1" })
        assertTrue(springboard.activators.any { it.resourceId == "trd" })
        assertTrue(springboard.guidanceData.none { it.resourceId == "res1" })
        assertTrue(springboard.guidanceData.any { it.resourceId == "trd" })
    }

    @Test
    fun `change_resource_id rejects duplicate new id`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ChangeResourceIdToolCallHandler().executeToolCallHandler(
            ChangeResourceIdToolCallHandlerRequest(tab_id = tabId, id = "res1", new_id = "res2", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("duplicate_id", result.code)
    }

    @Test
    fun `change_resource_id rejects blank new id`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ChangeResourceIdToolCallHandler().executeToolCallHandler(
            ChangeResourceIdToolCallHandlerRequest(tab_id = tabId, id = "res1", new_id = " ", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("blank_id", result.code)
    }

    @Test
    fun `remove_resource refuses when in use`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = RemoveResourceToolCallHandler().executeToolCallHandler(
            RemoveResourceToolCallHandlerRequest(tab_id = tabId, id = "res1", display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("in_use", result.code)
    }

    // ── environment CRUD ────────────────────────────────────────────────

    @Test
    fun `add_environment success`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = AddEnvironmentToolCallHandler().executeToolCallHandler(
            AddEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "stage", name = "Staging", display_message = "x"),
            ctx,
        )
        assertTrue(result.success)
        assertTrue(ctx.viewModel.springboardUnfiltered!!.environments.any { it.id == "stage" })
    }

    @Test
    fun `add_environment rejects reserved id ALL`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = AddEnvironmentToolCallHandler().executeToolCallHandler(
            AddEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "ALL", name = "All Envs", display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("reserved_id", result.code)
    }

    @Test
    fun `add_environment rejects reserved id case-insensitive`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = AddEnvironmentToolCallHandler().executeToolCallHandler(
            AddEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "all", name = "x", display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("reserved_id", result.code)
    }

    @Test
    fun `update_environment updates name`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = UpdateEnvironmentToolCallHandler().executeToolCallHandler(
            UpdateEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "common", name = "Common Renamed", display_message = "x"),
            ctx,
        )
        assertTrue(result.success)
        assertEquals(
            "Common Renamed",
            ctx.viewModel.springboardUnfiltered!!.environments.first { it.id == "common" }.name,
        )
    }

    @Test
    fun `remove_environment refuses when in use`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = RemoveEnvironmentToolCallHandler().executeToolCallHandler(
            RemoveEnvironmentToolCallHandlerRequest(tab_id = tabId, id = "common", display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("in_use", result.code)
    }

    // ── app group CRUD ─────────────────────────────────────────────────

    @Test
    fun `add_app_group adds and update_app_group changes the description`() = runTest {
        val (ctx, tabId) = loadedContext()
        val addResult = AddAppGroupToolCallHandler().executeToolCallHandler(
            AddAppGroupToolCallHandlerRequest(tab_id = tabId, id = "g1", description = "Group 1", display_message = "x"),
            ctx,
        )
        assertTrue(addResult.success)
        val updateResult = UpdateAppGroupToolCallHandler().executeToolCallHandler(
            UpdateAppGroupToolCallHandlerRequest(tab_id = tabId, id = "g1", description = "Group 1 (updated)", display_message = "x"),
            ctx,
        )
        assertTrue(updateResult.success)
        assertEquals(
            "Group 1 (updated)",
            ctx.viewModel.springboardUnfiltered!!.appGroups.first { it.id == "g1" }.description,
        )
    }

    @Test
    fun `remove_app_group refuses when an app references it`() = runTest {
        val groupedJson = """
        {
          "name": "with group",
          "appGroups": [{"id":"g1","description":"G"}],
          "apps": [{"id":"a1","name":"A1","appGroupId":"g1"}],
          "resources": [{"id":"r1","name":"R1"}],
          "environments": [{"id":"e1","name":"E1"}],
          "activators": [
            {"type":"url","appId":"a1","resourceId":"r1","environmentId":"e1","url":"https://x.example.com"}
          ]
        }
        """.trimIndent()
        val (ctx, tabId) = loadedContext(groupedJson)

        val result = RemoveAppGroupToolCallHandler().executeToolCallHandler(
            RemoveAppGroupToolCallHandlerRequest(tab_id = tabId, id = "g1", display_message = "x"),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("in_use", result.code)
    }
}
