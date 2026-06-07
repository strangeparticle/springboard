package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

internal class OpenFromUrlToolCallHandler : ToolCallHandler {
    override val providerToolId = "open_from_url"
    override val description = "Open a springboard from a URL."
    override val schema = requestSchema(OpenFromUrlToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: OpenFromUrlToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        return when (val result = context.viewModel.loadConfigFromSource(args.url, inNewTab = args.in_new_tab)) {
            is SpringboardViewModel.LoadResult.Success -> {
                context.markStateChanged()
                context.successResult()
            }
            is SpringboardViewModel.LoadResult.Failure -> context.errorResult(result.message, result.code)
        }
    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, OpenFromUrlToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
