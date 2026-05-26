package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.domain.mutator.changeResourceId
import com.strangeparticle.springboard.app.editio.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.editio.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.editio.applyMutation
import com.strangeparticle.springboard.app.editio.errorResult
import com.strangeparticle.springboard.app.editio.getSpringboardForTabOrError
import com.strangeparticle.springboard.app.editio.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.editio.handleMutationErrors

internal class ChangeResourceIdToolCallHandler : ToolCallHandler {
    override val providerToolId = "change_resource_id"
    override val description = "Change the id of an existing resource and rewrite all references to that resource id."
    override val schema = requestSchema(ChangeResourceIdToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: ChangeResourceIdToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { changeResourceId(springboard, args.id, args.new_id) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ChangeResourceIdToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
