package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.errorResult
import com.strangeparticle.springboard.app.luther.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.luther.handleMutationErrors
import com.strangeparticle.springboard.app.luther.successResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

internal class CreateSpringboardToolCallHandler : ToolCallHandler {
    override val providerToolId = "create_springboard"
    override val description = "Create a new unsaved springboard in a new tab and make it active."
    override val schema = requestSchema(CreateSpringboardToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(
        args: CreateSpringboardToolCallHandlerRequest,
        context: SpringboardToolCallExecutionContext,
    ): SpringboardToolCallHandlerResponse {
        return when (val result = context.viewModel.createUnsavedSpringboardTab(args.name)) {
            is SpringboardViewModel.LoadResult.Success -> {
                context.markStateChanged()
                context.successResult()
            }
            is SpringboardViewModel.LoadResult.Failure -> context.errorResult(result.message, result.code)
        }
    }

    override suspend fun executeToolCallHandler(
        toolCallId: String,
        argumentsAsJsonString: String,
        context: ToolCallExecutionContext,
    ): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, CreateSpringboardToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
