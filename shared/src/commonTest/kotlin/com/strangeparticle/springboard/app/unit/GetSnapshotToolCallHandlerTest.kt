package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.luther.toolcall.GetSnapshotToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GetSnapshotToolCallHandlerTest {
    private fun createContext() = SpringboardToolCallExecutionContextInMemoryFake(
        viewModel = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        ),
    )

    @Test
    fun `provider id and confirmation`() {
        val handler = GetSnapshotToolCallHandler()
        assertEquals("get_snapshot", handler.providerToolId)
        assertEquals(false, handler.requiresUserConfirmation)
    }

    @Test
    fun `returns snapshot json from context`() = runTest {
        val handler = GetSnapshotToolCallHandler()
        val context = createContext()
        val response = handler.executeToolCallHandler(
            toolCallId = "call-1",
            argumentsAsJsonString = "{}",
            context = context,
        )
        val content = response.toProviderMessageContent()
        // The snapshot serializes its `tabs` field; it is embedded in the response `message`.
        assertTrue(content.contains("tabs"), content)
    }
}
