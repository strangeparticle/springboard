package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.domain.mutator.changeEnvironmentId
import com.strangeparticle.springboard.app.luther.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.applyMutation
import com.strangeparticle.springboard.app.luther.errorResult
import com.strangeparticle.springboard.app.luther.getSpringboardForTabOrError
import com.strangeparticle.springboard.app.luther.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.luther.handleMutationErrors

internal class ChangeEnvironmentIdToolCallHandler : ToolCallHandler {
    override val providerToolId = "change_environment_id"
    override val description = "Change the id of an existing environment and rewrite all references to that environment id."
    override val schema = requestSchema(ChangeEnvironmentIdToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: ChangeEnvironmentIdToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { changeEnvironmentId(springboard, args.id, args.new_id) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ChangeEnvironmentIdToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
