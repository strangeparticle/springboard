package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

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
