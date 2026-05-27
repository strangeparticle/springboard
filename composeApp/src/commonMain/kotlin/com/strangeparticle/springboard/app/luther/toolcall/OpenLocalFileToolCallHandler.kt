package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

internal class OpenLocalFileToolCallHandler : ToolCallHandler {
    override val providerToolId = "open_local_file"
    override val description = "Open a local springboard file."
    override val schema = requestSchema(OpenLocalFileToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: OpenLocalFileToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        return when (val result = context.viewModel.loadConfigFromSource(args.path, inNewTab = args.in_new_tab)) {
            is SpringboardViewModel.LoadResult.Success -> {
                context.markStateChanged()
                context.successResult()
            }
            is SpringboardViewModel.LoadResult.Failure -> context.errorResult(result.message, result.code)
        }
    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, OpenLocalFileToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
