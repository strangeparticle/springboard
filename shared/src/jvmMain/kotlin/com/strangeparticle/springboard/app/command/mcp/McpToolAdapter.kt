package com.strangeparticle.springboard.app.command.mcp

import com.strangeparticle.luther.core.toolcall.ToolCallDispatcher
import com.strangeparticle.luther.core.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.core.toolcall.ToolCallExecutionResult
import com.strangeparticle.luther.core.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.core.toolcall.ToolCallRegistry
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.command.SpringboardCommandJson
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.types.error
import io.modelcontextprotocol.kotlin.sdk.types.success
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Converts a handler's full JSON Schema object into the SDK's [ToolSchema]. */
internal fun toToolSchema(schema: JsonObject): ToolSchema {
    val properties = schema["properties"] as? JsonObject
    val required = (schema["required"] as? JsonArray)?.map { it.jsonPrimitive.content }
    return ToolSchema(properties = properties, required = required)
}

/** Maps a Springboard tool response onto an MCP [CallToolResult]. */
internal fun toCallToolResult(response: ToolCallHandlerResponse): CallToolResult {
    val content = response.toProviderMessageContent(SpringboardCommandJson.json)
    val success = when (response) {
        is SpringboardToolCallHandlerResponse -> response.success
        is ToolCallExecutionResult -> response.success
        else -> true
    }
    return if (success) CallToolResult.success(content) else CallToolResult.error(content)
}

/**
 * Builds an MCP [Server] that advertises only the tools capability and registers every handler
 * in [toolCallRegistry], delegating execution to [toolCallDispatcher].
 */
internal fun buildSpringboardMcpServer(
    toolCallRegistry: ToolCallRegistry,
    toolCallDispatcher: ToolCallDispatcher,
    toolCallExecutionContext: ToolCallExecutionContext?,
    serverName: String,
    serverVersion: String,
): Server {
    val server = Server(
        serverInfo = Implementation(name = serverName, version = serverVersion),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false)),
        ),
    )
    toolCallRegistry.getHandlers().forEach { handler ->
        server.addTool(
            name = handler.providerToolId,
            description = handler.description,
            inputSchema = toToolSchema(handler.schema),
        ) { request ->
            if (handler.requiresUserConfirmation) {
                return@addTool CallToolResult.error(
                    "Tool '${handler.providerToolId}' requires user confirmation and cannot be " +
                        "executed over the MCP endpoint.",
                )
            }
            val context = toolCallExecutionContext
                ?: return@addTool CallToolResult.error(
                    "Tool execution is not available in this Springboard process.",
                )
            val argumentsJson = SpringboardCommandJson.json.encodeToString(
                request.arguments ?: JsonObject(emptyMap()),
            )
            val response = try {
                toolCallDispatcher.execute(
                    toolCallId = "mcp-${request.params.name}",
                    providerToolId = handler.providerToolId,
                    argumentsAsJsonString = argumentsJson,
                    context = context,
                )
            } catch (failure: Exception) {
                ToolCallExecutionResult(
                    success = false,
                    message = "Tool execution failed: ${failure.message ?: "unknown error"}",
                    code = "internal_error",
                )
            }
            toCallToolResult(response)
        }
    }
    return server
}
