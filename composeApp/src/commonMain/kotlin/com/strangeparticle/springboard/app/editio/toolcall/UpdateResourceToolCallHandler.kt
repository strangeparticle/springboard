package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.mutator.updateResource

internal class UpdateResourceToolCallHandler : ToolCallHandler {
    override val providerToolId = "update_resource"
    override val description = "Update an existing resource."
    override val schema = requestSchema(UpdateResourceToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: UpdateResourceToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { updateResource(springboard, args.id, args.name) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, UpdateResourceToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
