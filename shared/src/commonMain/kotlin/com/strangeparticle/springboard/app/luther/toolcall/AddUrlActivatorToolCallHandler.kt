package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.model.UrlActivator
import com.strangeparticle.springboard.app.domain.mutator.addActivator

internal class AddUrlActivatorToolCallHandler : ToolCallHandler {
    override val providerToolId = "add_url_activator"
    override val description = "Add a URL activator at a coordinate."
    override val schema = requestSchema(AddUrlActivatorToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: AddUrlActivatorToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { addActivator(springboard, UrlActivator(args.app_id, args.resource_id, args.environment_id, args.url)) }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddUrlActivatorToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
