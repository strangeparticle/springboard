package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.core.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.core.toolcall.ToolCallHandler
import com.strangeparticle.luther.core.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.core.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.getSpringboardToolCallExecutionContextOrThrow
import kotlinx.serialization.Serializable

/** Empty request: get_snapshot takes no arguments. Used only to derive the (empty) input schema. */
@Serializable
internal class GetSnapshotToolCallHandlerRequest

/**
 * Read-only tool that returns the current Springboard state snapshot. Springboard's in-app
 * assistant receives state via the `<current_state>` auto-injection in the conversation loop,
 * but external MCP agents run their own loop and must pull state explicitly — this tool is that
 * pull. It reuses the same snapshot source as the auto-injection so both paths see identical state.
 */
internal class GetSnapshotToolCallHandler : ToolCallHandler {
    override val providerToolId = "get_snapshot"

    override val description =
        "Returns the current Springboard state as JSON: open tabs, the active tab id, and each " +
            "tab's environments, apps, resources, and activators. Call this before acting (and " +
            "again after changes) to obtain current ids."

    override val schema = requestSchema(GetSnapshotToolCallHandlerRequest.serializer())

    override suspend fun executeToolCallHandler(
        toolCallId: String,
        argumentsAsJsonString: String,
        context: ToolCallExecutionContext,
    ): ToolCallHandlerResponse {
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        val snapshotJson = SpringboardAppSnapshot.capture(springboardContext.viewModel).toCompactJson()
        return SpringboardToolCallHandlerResponse(success = true, message = snapshotJson)
    }
}
