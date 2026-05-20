package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.editio.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.editio.errorResult
import com.strangeparticle.springboard.app.editio.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.editio.handleMutationErrors
import com.strangeparticle.springboard.app.editio.successResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

internal class CreateSpringboardToolCallHandler : ToolCallHandler {
    override val providerToolId = "create_springboard"
    override val description = "Create a new unsaved springboard in a new tab and make it active."
    override val schema = requestSchema(CreateSpringboardToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(
        args: CreateSpringboardToolCallHandlerRequest,
        context: SpringboardToolCallExecutionContext,
    ): SpringboardToolCallHandlerResponse {
        return when (val result = context.viewModel.createUnsavedSpringboardTab()) {
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
