package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.domain.undo.UndoRedoOutcome
import com.strangeparticle.springboard.app.luther.getSpringboardToolCallExecutionContextOrThrow
import com.strangeparticle.springboard.app.luther.successStatusResult
import kotlinx.serialization.Serializable

/** Empty request: undo takes no arguments. Used only to derive the (empty) input schema. */
@Serializable
internal class UndoToolCallHandlerRequest

/**
 * Undoes the most recent change to the active Springboard tab, reverting its state. Exposed as a
 * tool call so both the in-app assistant and external MCP/Command-API agents can trigger the
 * viewmodel's per-tab undo engine. Doing nothing when there is nothing to undo is a normal,
 * successful outcome (not an error); the result message conveys that case.
 */
internal class UndoToolCallHandler : ToolCallHandler {
    override val providerToolId = "undo"

    override val description =
        "Undo the most recent change to the active Springboard tab, reverting its state. Acts on " +
            "the active tab only. Does nothing if there is nothing to undo."

    override val schema = requestSchema(UndoToolCallHandlerRequest.serializer())

    override suspend fun executeToolCallHandler(
        toolCallId: String,
        argumentsAsJsonString: String,
        context: ToolCallExecutionContext,
    ): ToolCallHandlerResponse {
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return when (springboardContext.viewModel.undoActiveTab()) {
            UndoRedoOutcome.Undone -> {
                springboardContext.markStateChanged()
                successStatusResult("Undid last change.")
            }
            UndoRedoOutcome.NothingToUndo -> successStatusResult("Nothing to undo.")
            UndoRedoOutcome.Redone, UndoRedoOutcome.NothingToRedo ->
                error("undoActiveTab() returned an unexpected outcome")
        }
    }
}
