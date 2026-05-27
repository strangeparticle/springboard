package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.luther.toolcall.RespondWithMessageToolCallHandlerRequest
import com.strangeparticle.springboard.app.luther.toolcall.RespondWithMessageToolCallHandler
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [RespondWithMessageToolCallHandler]. Pure-prose tool — no viewmodel mutation,
 * no `markStateChanged()` flip.
 */
internal class RespondWithMessageToolCallHandlerTest {

    private fun createContext() = SpringboardToolCallExecutionContextInMemoryFake(
        viewModel = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        ),
    )

    @Test
    fun `respondWithMessage does not mutate viewmodel state`() = runTest {
        val context = createContext()
        context.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test.json")
        val initialAppCount = context.viewModel.springboardUnfiltered?.apps?.size ?: 0
        val initialDirty = context.viewModel.activeTab?.isDirty == true

        val result = RespondWithMessageToolCallHandler().executeToolCallHandler(
            args = RespondWithMessageToolCallHandlerRequest("here's an explanation of activators"),
            context = context,
        )

        assertTrue(result.success)
        assertEquals(initialAppCount, context.viewModel.springboardUnfiltered?.apps?.size,
            "respond_with_message must not change viewmodel state")
        assertEquals(initialDirty, context.viewModel.activeTab?.isDirty,
            "respond_with_message must not flip the dirty flag")
        assertEquals(0, context.stateChangedCount,
            "respond_with_message must not call markStateChanged() — there's no state change to signal")
        assertNull(result.state, "respond_with_message must return no snapshot")
    }
}
