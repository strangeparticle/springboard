package com.strangeparticle.luther.toolcall

import kotlinx.serialization.Serializable

@Serializable
internal data class ToolCallDebugDto(
    val toolCallId: String,
    val toolName: String,
    val argumentsAsJsonString: String,
) {
    companion object {
        fun from(toolCall: ToolCall): ToolCallDebugDto = ToolCallDebugDto(
            toolCallId = toolCall.toolCallId,
            toolName = toolCall.toolName,
            argumentsAsJsonString = toolCall.argumentsAsJsonString,
        )
    }
}
