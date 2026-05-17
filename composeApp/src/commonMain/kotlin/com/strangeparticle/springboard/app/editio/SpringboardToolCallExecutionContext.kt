package com.strangeparticle.springboard.app.editio

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.CompletableDeferred

/**
 * Springboard dependencies a [ToolCallHandler]'s `executeToolCallHandler(...)`
 * needs at runtime.
 *
 * The interface is small on purpose: tool calls should reach the viewmodel for state
 * reads and call dedicated helper functions for mutations. They should NOT touch
 * Compose state directly or know about the chat pane.
 *
 * Per spec §4.1.
 */
internal interface SpringboardToolCallExecutionContext : ToolCallExecutionContext {

    /** The app's single viewmodel. Tools read state from it and call mutator methods on it. */
    val viewModel: SpringboardViewModel

    /**
     * Mark the most recent mutation as state-changing. The session manager flips
     * its `stateChangedSinceLastSnapshotSent` flag so the next outgoing request
     * includes a fresh [SpringboardAppSnapshot]. Tools that mutate viewmodel state must
     * call this; tools that don't (e.g. `respond_with_message`) must not.
     */
    fun markStateChanged()

    /**
     * Wait for the user to approve or deny a confirmation-gated tool call. The
     * session manager appends a `ToolCall(state = ApprovalRequested(args))` part
     * to the chat transcript and registers a [CompletableDeferred] keyed by
     * [toolCallId]; clicking Apply / Cancel in the UI fulfills it. Returns `true`
     * for Apply, `false` for Cancel.
     */
    suspend fun awaitUserApproval(toolCallId: String): Boolean
}

// -- Convenience helpers for tools ---------------------------------------------

/**
 * Capture the current viewmodel state as a [SpringboardToolCallHandlerResponse].
 * Used by tool calls whose meaningful output is the updated app state.
 */
internal fun SpringboardToolCallExecutionContext.successResult(): SpringboardToolCallHandlerResponse {
    return SpringboardToolCallHandlerResponse(
        success = true,
        state = SpringboardAppSnapshot.capture(viewModel),
    )
}

/**
 * Return a structured tool-call error without pretending the error is app state.
 * Tool-call failures are provider tool results, not fields on [SpringboardAppSnapshot].
 */
internal fun SpringboardToolCallExecutionContext.errorResult(
    message: String,
    code: String,
): SpringboardToolCallHandlerResponse {
    return SpringboardToolCallHandlerResponse(
        success = false,
        message = message,
        code = code,
    )
}

internal fun SpringboardToolCallExecutionContext.errorResult(error: SpringboardMutationError): SpringboardToolCallHandlerResponse =
    errorResult(message = error.errorMessage, code = error.code)

/** Resolves [tabId] to its [Springboard], or throws a structured mutation error. */
internal fun SpringboardToolCallExecutionContext.getSpringboardForTabOrError(
    tabId: String,
): Springboard {
    val tab = viewModel.findTab(tabId)
        ?: throw SpringboardMutationError("No tab with id '$tabId'.", "missing_tab")
    val springboard = tab.springboard
        ?: throw SpringboardMutationError("Tab '$tabId' has no loaded springboard.", "tab_empty")
    return springboard
}

/**
 * Apply [springboard] to the target [tabId], mark dirty, mark state changed, and
 * return success with state.
 */
internal fun SpringboardToolCallExecutionContext.applyMutation(
    tabId: String,
    springboard: Springboard,
): SpringboardToolCallHandlerResponse {
    viewModel.replaceTabSpringboard(tabId, springboard)
    viewModel.markTabDirty(tabId)
    markStateChanged()
    return successResult()
}

internal fun SpringboardToolCallExecutionContext.applyMutation(
    tabId: String,
    mutate: () -> Springboard,
): SpringboardToolCallHandlerResponse = try {
    applyMutation(tabId, mutate())
} catch (e: SpringboardMutationError) {
    errorResult(e)
}

internal suspend fun SpringboardToolCallExecutionContext.handleMutationErrors(
    block: suspend () -> SpringboardToolCallHandlerResponse,
): SpringboardToolCallHandlerResponse = try {
    block()
} catch (e: SpringboardMutationError) {
    errorResult(e)
}

/** Result for non-snapshot tool calls whose main output is a success flag. */
internal fun successStatusResult(message: String? = null): SpringboardToolCallHandlerResponse =
    SpringboardToolCallHandlerResponse(success = true, message = message)

/** Error result for non-snapshot tool calls. */
internal fun errorStatusResult(message: String, code: String): SpringboardToolCallHandlerResponse =
    SpringboardToolCallHandlerResponse(success = false, code = code, message = message)

internal fun ToolCallExecutionContext.getSpringboardToolCallExecutionContextOrThrow(): SpringboardToolCallExecutionContext =
    this as? SpringboardToolCallExecutionContext
        ?: error("Expected SpringboardToolCallExecutionContext but got ${this::class.simpleName}.")
