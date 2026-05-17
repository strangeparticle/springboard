package com.strangeparticle.editio.toolcall

/** A single tool invocation requested by the model. */
internal data class ToolCall(
    val toolCallId: String,
    val toolName: String,
    val argumentsAsJsonString: String,
)
