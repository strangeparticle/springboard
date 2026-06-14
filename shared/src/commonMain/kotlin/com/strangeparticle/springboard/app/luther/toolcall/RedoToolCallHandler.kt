package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.core.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.core.toolcall.ToolCallHandler
import com.strangeparticle.luther.core.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.core.toolcall.requestSchema
import com.strangeparticle.springboard.app.domain.undo.UndoRedoOutcome
import com.strangeparticle.springboard.app.luther.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.luther.successStatusResult
import kotlinx.serialization.Serializable

/** Empty request: redo takes no arguments. Used only to derive the (empty) input schema. */
@Serializable
internal class RedoToolCallHandlerRequest

/**
 * Redoes the most recently undone change to the active Springboard tab, reapplying its state.
 * Exposed as a tool call so both the in-app assistant and external MCP/Command-API agents can
 * trigger the viewmodel's per-tab redo engine. Doing nothing when there is nothing to redo is a
 * normal, successful outcome (not an error); the result message conveys that case.
 */
internal class RedoToolCallHandler : ToolCallHandler {
    override val providerToolId = "redo"

    override val description =
        "Redo the most recently undone change to the active Springboard tab, reapplying its " +
            "state. Acts on the active tab only. Does nothing if there is nothing to redo."

    override val schema = requestSchema(RedoToolCallHandlerRequest.serializer())

    override suspend fun executeToolCallHandler(
        toolCallId: String,
        argumentsAsJsonString: String,
        context: ToolCallExecutionContext,
    ): ToolCallHandlerResponse {
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return when (springboardContext.viewModel.redoActiveTab()) {
            UndoRedoOutcome.Redone -> {
                springboardContext.markStateChanged()
                successStatusResult("Redid last change.")
            }
            UndoRedoOutcome.NothingToRedo -> successStatusResult("Nothing to redo.")
            UndoRedoOutcome.Undone, UndoRedoOutcome.NothingToUndo ->
                error("redoActiveTab() returned an unexpected outcome")
        }
    }
}
