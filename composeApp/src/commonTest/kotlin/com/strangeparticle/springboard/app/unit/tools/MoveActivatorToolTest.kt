package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.editio.toolcall.MoveActivatorToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.toolcall.MoveActivatorToolCallHandler
import com.strangeparticle.springboard.app.domain.model.Coordinate
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MoveActivatorToolCallHandlerTest {

    /**
     * Builds a fresh context with two tabs already loaded, returning the (ctx,
     * sourceTabId, destTabId) triple. The source tab is loaded with the
     * MULTI_ENV_WITH_COMMON fixture; the destination is loaded with a custom
     * [destJson] (or a default that has the same apps/resources/envs).
     */
    private suspend fun twoTabContext(
        destJson: String = TestFixtureJson.MULTI_ENV_WITH_COMMON,
    ): Triple<SpringboardToolCallExecutionContextInMemoryFake, String, String> {
        val files = mapOf(
            "/source.json" to TestFixtureJson.MULTI_ENV_WITH_COMMON,
            "/dest.json" to destJson,
        )
        val fileService = PlatformFileContentServiceInMemoryFake()
        files.forEach { (path, contents) -> fileService.fileContents[path] = contents }
        val loader = SpringboardContentLoaderInMemoryFake(fileService)
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
            contentLoader = loader,
        )
        vm.loadConfigFromSource("/source.json", inNewTab = false)
        val sourceTabId = vm.activeTabId
        vm.loadConfigFromSource("/dest.json", inNewTab = true)
        val destTabId = vm.activeTabId
        return Triple(SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm), sourceTabId, destTabId)
    }

    @Test
    fun `move_activator removes from source and adds to destination`() = runTest {
        // Destination fixture lacks (preprod, app1, res1) — so the move should succeed.
        val destJson = """
        {
          "name": "dest",
          "appGroups": [],
          "apps": [{"id":"app1","name":"App One"},{"id":"app2","name":"App Two"}],
          "resources": [{"id":"res1","name":"R1"},{"id":"res2","name":"R2"}],
          "environments": [
            {"id":"common","name":"Common"},
            {"id":"preprod","name":"Preprod"},
            {"id":"prod","name":"Prod"}
          ],
          "activators": []
        }
        """.trimIndent()
        val (ctx, sourceTabId, destTabId) = twoTabContext(destJson)
        val coord = Coordinate("preprod", "app1", "res1")

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = destTabId,
                app_id = "app1",
                resource_id = "res1",
                environment_id = "preprod",
                display_message = "moved",
            ),
            ctx,
        )

        assertTrue(result.success)
        // Source tab no longer has the activator.
        val sourceSpringboard = ctx.viewModel.findTab(sourceTabId)!!.springboardUnfiltered!!
        assertNull(sourceSpringboard.indexes.activatorByCoordinate[coord])
        // Destination tab has the activator.
        val destSpringboard = ctx.viewModel.findTab(destTabId)!!.springboardUnfiltered!!
        assertNotNull(destSpringboard.indexes.activatorByCoordinate[coord])
        // Both tabs marked dirty.
        assertTrue(ctx.viewModel.findTab(sourceTabId)!!.isDirty)
        assertTrue(ctx.viewModel.findTab(destTabId)!!.isDirty)
    }

    @Test
    fun `move_activator refuses when source tab is missing`() = runTest {
        val (ctx, _, destTabId) = twoTabContext()

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = "no_such_tab",
                to_tab_id = destTabId,
                app_id = "app1", resource_id = "res1", environment_id = "preprod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_tab", result.code)
    }

    @Test
    fun `move_activator refuses when destination tab is missing`() = runTest {
        val (ctx, sourceTabId, _) = twoTabContext()

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = "no_such_tab",
                app_id = "app1", resource_id = "res1", environment_id = "preprod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_tab", result.code)
    }

    @Test
    fun `move_activator refuses when source coordinate has no activator`() = runTest {
        val (ctx, sourceTabId, destTabId) = twoTabContext()

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = destTabId,
                // (prod, app2, res2) is unused in MULTI_ENV_WITH_COMMON.
                app_id = "app2", resource_id = "res2", environment_id = "prod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_target", result.code)
    }

    @Test
    fun `move_activator refuses when destination is missing required references`() = runTest {
        val destJson = """
        {
          "name": "dest with no app1",
          "appGroups": [],
          "apps": [{"id":"someOtherApp","name":"Other"}],
          "resources": [{"id":"res1","name":"R1"}],
          "environments": [{"id":"preprod","name":"Preprod"}],
          "activators": []
        }
        """.trimIndent()
        val (ctx, sourceTabId, destTabId) = twoTabContext(destJson)

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = destTabId,
                app_id = "app1", resource_id = "res1", environment_id = "preprod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_reference", result.code)
        // Source must NOT have been mutated since the destination add failed.
        val coord = Coordinate("preprod", "app1", "res1")
        val sourceSpringboard = ctx.viewModel.findTab(sourceTabId)!!.springboardUnfiltered!!
        assertNotNull(sourceSpringboard.indexes.activatorByCoordinate[coord],
            "Source tab must be untouched when the destination add fails")
    }

    @Test
    fun `move_activator refuses when destination coordinate is occupied`() = runTest {
        val destJson = """
        {
          "name": "dest with conflict",
          "appGroups": [],
          "apps": [{"id":"app1","name":"App One"}],
          "resources": [{"id":"res1","name":"R1"}],
          "environments": [{"id":"preprod","name":"Preprod"}],
          "activators": [
            {"type":"url","appId":"app1","resourceId":"res1","environmentId":"preprod","url":"https://existing.example.com"}
          ]
        }
        """.trimIndent()
        val (ctx, sourceTabId, destTabId) = twoTabContext(destJson)

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = destTabId,
                app_id = "app1", resource_id = "res1", environment_id = "preprod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("duplicate_coordinate", result.code)
        // Source untouched.
        val coord = Coordinate("preprod", "app1", "res1")
        assertNotNull(ctx.viewModel.findTab(sourceTabId)!!.springboardUnfiltered!!.indexes.activatorByCoordinate[coord])
    }

    @Test
    fun `move_activator refuses when source and destination are the same tab`() = runTest {
        val (ctx, sourceTabId, _) = twoTabContext()

        val result = MoveActivatorToolCallHandler().executeToolCallHandler(
            MoveActivatorToolCallHandlerRequest(
                from_tab_id = sourceTabId,
                to_tab_id = sourceTabId,
                app_id = "app1", resource_id = "res1", environment_id = "preprod",
                display_message = "x",
            ),
            ctx,
        )

        assertFalse(result.success)
        assertEquals("same_tab", result.code)
    }
}
