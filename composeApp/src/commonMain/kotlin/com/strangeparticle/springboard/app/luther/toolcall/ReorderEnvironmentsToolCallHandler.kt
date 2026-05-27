package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.mutator.reorderEnvironments

internal class ReorderEnvironmentsToolCallHandler : ToolCallHandler {
    override val providerToolId = "reorder_environments"
    override val description = "Replace the ordering of environments."
    override val schema = requestSchema(ReorderEnvironmentsToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: ReorderEnvironmentsToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { reorderEnvironments(springboard, args.ordered_ids) }
    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ReorderEnvironmentsToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
