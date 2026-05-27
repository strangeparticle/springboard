package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError

import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.domain.mutator.addApp

internal class AddAppToolCallHandler : ToolCallHandler {
    override val providerToolId = "add_app"
    override val description = "Add a new app to the springboard."
    override val schema = requestSchema(AddAppToolCallHandlerRequest.serializer())

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddAppToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }

    suspend fun executeToolCallHandler(args: AddAppToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val springboard = try { context.getSpringboardForTabOrError(args.tab_id) } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        return context.applyMutation(args.tab_id) { addApp(springboard, App(id = args.id, name = args.name, appGroupId = args.app_group_id)) }
    }

}
