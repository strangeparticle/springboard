package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.core.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.core.toolcall.ToolCallHandler
import com.strangeparticle.luther.core.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.core.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.core.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

internal class RespondWithMessageToolCallHandler : ToolCallHandler {
    override val providerToolId = "respond_with_message"
    override val description = "Return a prose response to the user without mutating state."
    override val schema = requestSchema(RespondWithMessageToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: RespondWithMessageToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse =
        SpringboardToolCallHandlerResponse(
            success = true,
            message = args.display_message,
            endsTurn = true,
            transcriptOutput = args.display_message,
        )
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, RespondWithMessageToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
