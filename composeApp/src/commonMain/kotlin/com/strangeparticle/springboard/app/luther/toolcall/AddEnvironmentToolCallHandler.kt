package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.model.Environment
import com.strangeparticle.springboard.app.domain.mutator.addEnvironment

internal class AddEnvironmentToolCallHandler : ToolCallHandler {
    override val providerToolId = "add_environment"
    override val description = "Add a new environment to the springboard."
    override val schema = requestSchema(AddEnvironmentToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: AddEnvironmentToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { addEnvironment(springboard, Environment(id = args.id, name = args.name)) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddEnvironmentToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
