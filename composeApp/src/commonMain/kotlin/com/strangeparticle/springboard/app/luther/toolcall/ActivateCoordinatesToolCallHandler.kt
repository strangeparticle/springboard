package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

internal class ActivateCoordinatesToolCallHandler : ToolCallHandler {
    override val providerToolId = "activate_coordinates"
    override val description = "Activate a batch of coordinates as a single multi-selection within one tab — mirrors shift-select activation. Use when the user asks to open multiple specific entries together."
    override val schema = requestSchema(ActivateCoordinatesToolCallHandlerRequest.serializer())

    fun executeToolCallHandler(args: ActivateCoordinatesToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val outcome = context.viewModel.activateCoordinatesFromAssistant(args.tab_id, args.coordinates)
        return outcome.toToolCallResponse(args.tab_id)
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ActivateCoordinatesToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
