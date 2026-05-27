package com.strangeparticle.springboard.app.unit

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallDispatcher
import com.strangeparticle.luther.toolcall.ToolCallExecutionResult
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.ToolCallRegistry
import com.strangeparticle.luther.toolcall.requestSchema
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class ToolCallExecutorTest {

    private class TestToolCallExecutionContext : ToolCallExecutionContext

    private data class TestToolCallHandlerResponse(
        val message: String,
    ) : ToolCallHandlerResponse {
        override fun toProviderMessageContent(json: Json): String = message
    }

    @Serializable
    private data class TestToolCallHandlerRequest(val display_message: String)

    private class TestToolCallHandler(
        val onExecute: (String, String, ToolCallExecutionContext) -> Unit = { _, _, _ -> },
    ) : ToolCallHandler {
        override val providerToolId = "test_tool"
        override val description = "Test tool."
        override val schema = requestSchema(TestToolCallHandlerRequest.serializer())
        override val requiresUserConfirmation = false

        override suspend fun executeToolCallHandler(
            toolCallId: String,
            argumentsAsJsonString: String,
            context: ToolCallExecutionContext,
        ): ToolCallHandlerResponse {
            onExecute(toolCallId, argumentsAsJsonString, context)
            return TestToolCallHandlerResponse("ok")
        }
    }

    private class InvalidArgumentsToolCallHandler : ToolCallHandler {
        override val providerToolId = "invalid_arguments_tool"
        override val description = "Invalid arguments tool."
        override val schema = requestSchema(TestToolCallHandlerRequest.serializer())
        override val requiresUserConfirmation = false

        override suspend fun executeToolCallHandler(
            toolCallId: String,
            argumentsAsJsonString: String,
            context: ToolCallExecutionContext,
        ): ToolCallHandlerResponse {
            throw SerializationException("missing display_message")
        }
    }

    @Test
    fun `registry exposes definitions without executing handlers`() {
        val registry = ToolCallRegistry()
        registry.register(TestToolCallHandler())

        val definitions = registry.getDefinitions()

        assertEquals(1, definitions.size)
        assertEquals("test_tool", definitions.single().name)
        assertEquals("Test tool.", definitions.single().description)
        assertTrue(registry.isRegistered("test_tool"))
        assertFalse(registry.isRegistered("other_tool"))
    }

    @Test
    fun `executor dispatches registered tool with raw arguments and context`() = runTest {
        val context = TestToolCallExecutionContext()
        var executedArguments: String? = null
        var executedContext: ToolCallExecutionContext? = null
        val registry = ToolCallRegistry()
        registry.register(TestToolCallHandler { _, argumentsAsJsonString, executionContext ->
            executedArguments = argumentsAsJsonString
            executedContext = executionContext
        })
        val executor = ToolCallDispatcher(registry)

        val result = executor.execute(
            toolCallId = "call-1",
            providerToolId = "test_tool",
            argumentsAsJsonString = "{\"display_message\":\"hello\"}",
            context = context,
        )

        assertEquals(TestToolCallHandlerResponse("ok"), result)
        assertEquals("{\"display_message\":\"hello\"}", executedArguments)
        assertSame(context, executedContext)
    }

    @Test
    fun `executor returns generic error result for unknown tool`() = runTest {
        val executor = ToolCallDispatcher(ToolCallRegistry())

        val result = executor.execute(
            toolCallId = "call-missing",
            providerToolId = "missing_tool",
            argumentsAsJsonString = "{}",
            context = TestToolCallExecutionContext(),
        )

        assertEquals(
            ToolCallExecutionResult(
                success = false,
                message = "Unknown tool: 'missing_tool'",
                code = "unknown_tool",
            ),
            result,
        )
    }

    @Test
    fun `executor returns generic error result for invalid arguments`() = runTest {
        val registry = ToolCallRegistry()
        registry.register(InvalidArgumentsToolCallHandler())
        val executor = ToolCallDispatcher(registry)

        val result = executor.execute(
            toolCallId = "call-invalid",
            providerToolId = "invalid_arguments_tool",
            argumentsAsJsonString = "{}",
            context = TestToolCallExecutionContext(),
        )

        assertEquals(
            ToolCallExecutionResult(
                success = false,
                message = "Invalid arguments for 'invalid_arguments_tool': missing display_message",
                code = "invalid_arguments",
            ),
            result,
        )
    }

    @Test
    fun `executor forwards toolCallId to handler`() = runTest {
        var receivedToolCallId: String? = null
        val registry = ToolCallRegistry()
        registry.register(TestToolCallHandler { toolCallId, _, _ ->
            receivedToolCallId = toolCallId
        })
        val executor = ToolCallDispatcher(registry)

        executor.execute(
            toolCallId = "call-forwarded",
            providerToolId = "test_tool",
            argumentsAsJsonString = "{\"display_message\":\"x\"}",
            context = TestToolCallExecutionContext(),
        )

        assertEquals("call-forwarded", receivedToolCallId)
    }
}
