package com.strangeparticle.springboard.app.unit

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallDispatcher
import com.strangeparticle.editio.toolcall.ToolCallExecutionResult
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.ToolCallRegistry
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.springboard.app.editio.SpringboardToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.editio.successResult
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for [ToolCallRegistry].
 */
internal class ToolCallRegistryTest {

    @Serializable
    private data class HelloToolCallHandlerRequest(val display_message: String)

    /** Minimal tool used as the registry's payload across tests. */
    private class HelloToolCallHandler(val onExecute: () -> Unit = {}) : ToolCallHandler {
        override val providerToolId = "hello"
        override val description = "Say hello."
        override val schema = requestSchema(HelloToolCallHandlerRequest.serializer())
        override suspend fun executeToolCallHandler(
            toolCallId: String,
            argumentsAsJsonString: String,
            context: ToolCallExecutionContext,
        ): ToolCallHandlerResponse {
            val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
            decodeToolCallHandlerRequest(argumentsAsJsonString, HelloToolCallHandlerRequest.serializer())
            onExecute()
            return springboardContext.successResult()
        }
    }

    private fun createContext() = SpringboardToolCallExecutionContextInMemoryFake(
        viewModel = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        ),
    )

    @Test
    fun `register makes tool available in getDefinitions`() {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        val defs = registry.getDefinitions()

        assertEquals(1, defs.size)
        assertEquals("hello", defs[0].name)
        assertEquals("Say hello.", defs[0].description)
    }

    @Test
    fun `registering the same name twice throws`() {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        val ex = kotlin.runCatching { registry.register(HelloToolCallHandler()) }.exceptionOrNull()

        assertTrue(ex is IllegalArgumentException)
    }

    @Test
    fun `dispatch with valid args calls executeToolCallHandler and returns success`() = runTest {
        var executed = false
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler(onExecute = { executed = true }))

        val result = ToolCallDispatcher(registry).execute(
            toolCallId = "call-1",
            providerToolId = "hello",
            argumentsAsJsonString = """{"display_message":"hi"}""",
            context = createContext(),
        )

        assertTrue(executed, "executeToolCallHandler() should have been called")
        assertIs<SpringboardToolCallHandlerResponse>(result)
        assertTrue(result.success, "valid call should produce a success result")
    }

    @Test
    fun `successful state-changing tool omits state from provider and transcript output`() = runTest {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        val result = ToolCallDispatcher(registry).execute(
            toolCallId = "call-1",
            providerToolId = "hello",
            argumentsAsJsonString = """{"display_message":"hi"}""",
            context = createContext(),
        )

        assertIs<SpringboardToolCallHandlerResponse>(result)
        assertTrue(result.success)
        val providerMessageContent = result.toProviderMessageContent(Json)
        assertEquals("""{"success":true}""", providerMessageContent)
        assertEquals("Applied.", result.toTranscriptOutput(providerMessageContent))
        assertFalse(providerMessageContent.contains("state"))
        assertFalse(result.toTranscriptOutput(providerMessageContent).contains("state"))
    }

    @Test
    fun `dispatch unknown tool returns an error result without throwing`() = runTest {
        val registry = ToolCallRegistry()

        val result = ToolCallDispatcher(registry).execute(
            toolCallId = "call-x",
            providerToolId = "no_such_tool",
            argumentsAsJsonString = "{}",
            context = createContext(),
        )

        assertIs<ToolCallExecutionResult>(result)
        assertFalse(result.success, "unknown-tool dispatch should return an error result")
        assertEquals("unknown_tool", result.code)
        assertTrue(result.message?.contains("no_such_tool") == true)
    }

    @Test
    fun `dispatch with malformed args returns an error result`() = runTest {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        val result = ToolCallDispatcher(registry).execute(
            toolCallId = "call-1",
            providerToolId = "hello",
            argumentsAsJsonString = "{}",
            context = createContext(),
        )

        assertIs<ToolCallExecutionResult>(result)
        assertFalse(result.success, "missing-arg dispatch should return an error result")
        assertEquals("invalid_arguments", result.code)
    }

    @Test
    fun `dispatch with wrong-typed args returns an error result`() = runTest {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        val result = ToolCallDispatcher(registry).execute(
            toolCallId = "call-1",
            providerToolId = "hello",
            argumentsAsJsonString = """{"display_message":42}""",
            context = createContext(),
        )

        assertIs<ToolCallExecutionResult>(result)
        assertFalse(result.success)
        assertEquals("invalid_arguments", result.code)
    }

    @Test
    fun `isRegistered reports correctly`() {
        val registry = ToolCallRegistry()
        registry.register(HelloToolCallHandler())

        assertTrue(registry.isRegistered("hello"))
        assertFalse(registry.isRegistered("goodbye"))
    }
}
