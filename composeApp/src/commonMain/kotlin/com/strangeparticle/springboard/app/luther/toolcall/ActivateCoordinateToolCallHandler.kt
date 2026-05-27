package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.app.viewmodel.AssistantActivationOutcome

internal class ActivateCoordinateToolCallHandler : ToolCallHandler {
    override val providerToolId = "activate_coordinate"
    override val description = "Activate one entry by (environment_id, app_id, resource_id) — opens its URL or runs its command. Pass the tab the user named, or the active tab's id from the latest snapshot."
    override val schema = requestSchema(ActivateCoordinateToolCallHandlerRequest.serializer())

    fun executeToolCallHandler(args: ActivateCoordinateToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val coordinate = Coordinate(args.environment_id, args.app_id, args.resource_id)
        val outcome = context.viewModel.activateCoordinateFromAssistant(args.tab_id, coordinate)
        return outcome.toToolCallResponse(args.tab_id)
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ActivateCoordinateToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
