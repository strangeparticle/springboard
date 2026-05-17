package com.strangeparticle.editio.toolcall

import kotlinx.serialization.Serializable

/** Generic result for dispatcher-level tool-call execution outcomes. */
@Serializable
internal data class ToolCallExecutionResult(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
) : ToolCallHandlerResponse
