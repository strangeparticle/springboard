package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

import com.strangeparticle.springboard.app.viewmodel.SaveResult

internal class SaveSpringboardToolCallHandler : ToolCallHandler {
    override val providerToolId = "save_springboard"
    override val description = "Save a local-file-backed springboard after explicit user approval."
    override val requiresUserConfirmation: Boolean = true
    override val schema = requestSchema(SaveSpringboardToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(toolCallId: String, args: SaveSpringboardToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val targetTab = context.viewModel.findTab(args.tab_id)
            ?: return errorStatusResult("No tab with id '${args.tab_id}'.", "missing_tab")
        val springboard = targetTab.springboard
        if (springboard == null) {
            return errorStatusResult("Tab '${args.tab_id}' has no loaded springboard to save.", "tab_empty")
        }
        val source = targetTab.source
        if (source == null) {
            return errorStatusResult("Save in place is not supported for this tab's source.", "not_supported_for_source")
        }
        val loweredSource = source.lowercase()
        if (loweredSource.startsWith("http://") || loweredSource.startsWith("https://")) {
            return errorStatusResult("Save in place is not supported for this tab's source.", "not_supported_for_source")
        }
        val approved = context.awaitUserApproval(toolCallId)
        if (!approved) return errorStatusResult("User declined to save the springboard.", "user_declined")

        val saveOutcome = context.viewModel.saveTab(args.tab_id)

        return when (saveOutcome) {
            is SaveResult.Success -> {
                context.markStateChanged()
                successStatusResult()
            }
            is SaveResult.WriteFailed -> errorStatusResult(
                message = "Failed to write '${saveOutcome.path}': ${saveOutcome.errorMessage}",
                code = "write_failed",
            )
            SaveResult.NotSupportedForSource -> errorStatusResult(
                message = "Save in place is not supported for this tab's source.",
                code = "not_supported_for_source",
            )
            SaveResult.NoSpringboard -> errorStatusResult(
                message = "Tab '${args.tab_id}' has no loaded springboard to save.",
                code = "tab_empty",
            )
        }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, SaveSpringboardToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(toolCallId, args, springboardContext) }
    }
}
