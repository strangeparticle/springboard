package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

internal class ActivateColumnToolCallHandler : ToolCallHandler {
    override val providerToolId = "activate_column"
    override val description = "Activate every resource's activator for a given app column in the named (environment_id, app_id) section. Uses the all-envs fallback for cells that don't have an env-specific activator."
    override val schema = requestSchema(ActivateColumnToolCallHandlerRequest.serializer())

    fun executeToolCallHandler(args: ActivateColumnToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val outcome = context.viewModel.activateColumnFromAssistant(args.tab_id, args.environment_id, args.app_id)
        return outcome.toToolCallResponse(args.tab_id)
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ActivateColumnToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
