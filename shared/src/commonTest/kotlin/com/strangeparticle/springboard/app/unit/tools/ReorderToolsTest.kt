package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.luther.toolcall.ReorderActivatorsToolCallHandlerRequest
import com.strangeparticle.springboard.app.luther.toolcall.ReorderActivatorsToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ReorderAppsToolCallHandlerRequest
import com.strangeparticle.springboard.app.luther.toolcall.ReorderAppsToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ReorderEnvironmentsToolCallHandlerRequest
import com.strangeparticle.springboard.app.luther.toolcall.ReorderEnvironmentsToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ReorderResourcesToolCallHandlerRequest
import com.strangeparticle.springboard.app.luther.toolcall.ReorderResourcesToolCallHandler
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

internal class ReorderToolsTest {

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
    fun `reorder_apps reorders the apps list`() = runTest {
        val (ctx, tabId) = loadedContext()
        // Original order: app1, app2.

        val result = ReorderAppsToolCallHandler().executeToolCallHandler(
            ReorderAppsToolCallHandlerRequest(
                tab_id = tabId,
                ordered_ids = listOf("app2", "app1"),
                display_message = "swapped",
            ),
            ctx,
        )

        assertTrue(result.success)
        assertEquals(listOf("app2", "app1"), ctx.viewModel.springboardUnfiltered!!.apps.map { it.id })
        assertTrue(ctx.viewModel.activeTab!!.isDirty)
    }

    @Test
    fun `reorder_apps rejects partial list`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderAppsToolCallHandler().executeToolCallHandler(
            ReorderAppsToolCallHandlerRequest(tab_id = tabId, ordered_ids = listOf("app1"), display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("wrong_size", result.code)
    }

    @Test
    fun `reorder_apps rejects duplicate id`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderAppsToolCallHandler().executeToolCallHandler(
            ReorderAppsToolCallHandlerRequest(tab_id = tabId, ordered_ids = listOf("app1", "app1"), display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("duplicate_id", result.code)
    }

    @Test
    fun `reorder_apps rejects unknown id`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderAppsToolCallHandler().executeToolCallHandler(
            ReorderAppsToolCallHandlerRequest(tab_id = tabId, ordered_ids = listOf("app1", "app99"), display_message = "x"),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("unknown_id", result.code)
    }

    @Test
    fun `reorder_resources reorders the resources list`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderResourcesToolCallHandler().executeToolCallHandler(
            ReorderResourcesToolCallHandlerRequest(tab_id = tabId, ordered_ids = listOf("res2", "res1"), display_message = "x"),
            ctx,
        )
        assertTrue(result.success)
        assertEquals(listOf("res2", "res1"), ctx.viewModel.springboardUnfiltered!!.resources.map { it.id })
    }

    @Test
    fun `reorder_environments reorders the environments list`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderEnvironmentsToolCallHandler().executeToolCallHandler(
            ReorderEnvironmentsToolCallHandlerRequest(
                tab_id = tabId,
                ordered_ids = listOf("prod", "preprod", "common"),
                display_message = "x",
            ),
            ctx,
        )
        assertTrue(result.success)
        assertEquals(
            listOf("prod", "preprod", "common"),
            ctx.viewModel.springboardUnfiltered!!.environments.map { it.id },
        )
    }

    @Test
    fun `reorder_activators reorders by coordinates`() = runTest {
        val (ctx, tabId) = loadedContext()
        val originalCoordinates = ctx.viewModel.springboardUnfiltered!!.activators.map {
            Coordinate(it.environmentId, it.appId, it.resourceId)
        }
        val reversed = originalCoordinates.reversed()

        val result = ReorderActivatorsToolCallHandler().executeToolCallHandler(
            ReorderActivatorsToolCallHandlerRequest(
                tab_id = tabId,
                ordered_coordinates = reversed,
                display_message = "x",
            ),
            ctx,
        )

        assertTrue(result.success)
        val newCoordinates = ctx.viewModel.springboardUnfiltered!!.activators.map {
            Coordinate(it.environmentId, it.appId, it.resourceId)
        }
        assertEquals(reversed, newCoordinates)
    }

    @Test
    fun `reorder_activators rejects partial coordinate list`() = runTest {
        val (ctx, tabId) = loadedContext()
        val result = ReorderActivatorsToolCallHandler().executeToolCallHandler(
            ReorderActivatorsToolCallHandlerRequest(
                tab_id = tabId,
                ordered_coordinates = listOf(
                    Coordinate(environmentId = "common", appId = "app1", resourceId = "res1"),
                ),
                display_message = "x",
            ),
            ctx,
        )
        assertFalse(result.success)
        assertEquals("wrong_size", result.code)
    }
}
