package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.mutator.addActivator

internal class AddCommandActivatorToolCallHandler : ToolCallHandler {
    override val providerToolId = "add_command_activator"
    override val description = "Add a command activator at a coordinate."
    override val schema = requestSchema(AddCommandActivatorToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: AddCommandActivatorToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { addActivator(springboard, CommandActivator(args.app_id, args.resource_id, args.environment_id, args.command_template)) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddCommandActivatorToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
