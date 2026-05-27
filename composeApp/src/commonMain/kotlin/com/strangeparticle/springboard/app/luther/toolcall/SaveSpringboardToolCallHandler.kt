package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

import com.strangeparticle.springboard.app.viewmodel.SaveResult

internal class SaveSpringboardToolCallHandler : ToolCallHandler {
    override val providerToolId = "save_springboard"
    override val description = "Save a local-file-backed springboard after explicit user approval."
    override val requiresUserConfirmation: Boolean = true
    override val schema = requestSchema(SaveSpringboardToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(toolCallId: String, args: SaveSpringboardToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val targetTab = context.viewModel.findTab(args.tab_id)
            ?: return errorStatusResult("No tab with id '${args.tab_id}'.", "missing_tab")
        val springboard = targetTab.springboardUnfiltered
        if (springboard == null) {
            return errorStatusResult("Tab '${args.tab_id}' has no loaded springboard to save.", "tab_empty")
        }
        val source = targetTab.source
        if (source == null) {
            return errorStatusResult("Save in place is not supported for this tab's source.", "not_supported_for_source")
        }
        val loweredSource = source.lowercase()
        val isHttpSource = loweredSource.startsWith("http://") || loweredSource.startsWith("https://")
        if (isHttpSource && targetTab.s3AwsProfile == null) {
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
            is SaveResult.Conflict -> errorStatusResult(
                message = "S3 conflict for '${saveOutcome.sourceUrl}': ${saveOutcome.message}",
                code = "s3_conflict",
            )
            is SaveResult.Denied -> errorStatusResult(
                message = "S3 access denied for '${saveOutcome.sourceUrl}': ${saveOutcome.message}",
                code = "s3_denied",
            )
        }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, SaveSpringboardToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(toolCallId, args, springboardContext) }
    }
}
