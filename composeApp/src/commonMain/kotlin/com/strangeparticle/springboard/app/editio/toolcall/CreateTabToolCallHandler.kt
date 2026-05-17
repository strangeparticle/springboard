package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

internal class CreateTabToolCallHandler : ToolCallHandler {
    override val providerToolId = "create_tab"
    override val description = "Create a new empty tab and make it active."
    override val schema = requestSchema(CreateTabToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: CreateTabToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        context.viewModel.createTab() ?: return context.errorResult(
            message = "Cannot create a new tab — tab limit reached.",
            code = "tab_limit_reached",
        )
        context.markStateChanged()
        return context.successResult()
    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, CreateTabToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
