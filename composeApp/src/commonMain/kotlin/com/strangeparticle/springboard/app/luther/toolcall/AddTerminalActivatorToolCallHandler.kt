package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.model.TerminalActivator
import com.strangeparticle.springboard.app.domain.mutator.addActivator

internal class AddTerminalActivatorToolCallHandler : ToolCallHandler {
    override val providerToolId = "add_terminal_activator"
    override val description = "Add a terminal activator at a coordinate. Opens a terminal at a working directory, optionally running a command."
    override val schema = requestSchema(AddTerminalActivatorToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: AddTerminalActivatorToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) {
            addActivator(
                springboard,
                TerminalActivator(args.app_id, args.resource_id, args.environment_id, args.working_directory, args.command),
            )
        }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddTerminalActivatorToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
