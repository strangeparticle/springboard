package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

internal class ActivateRowToolCallHandler : ToolCallHandler {
    override val providerToolId = "activate_row"
    override val description = "Activate every app's activator for a given resource row in the named (environment_id, resource_id) section. Uses the all-envs fallback for cells that don't have an env-specific activator."
    override val schema = requestSchema(ActivateRowToolCallHandlerRequest.serializer())

    fun executeToolCallHandler(args: ActivateRowToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val outcome = context.viewModel.activateRowFromAssistant(args.tab_id, args.environment_id, args.resource_id)
        return outcome.toToolCallResponse(args.tab_id)
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ActivateRowToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
