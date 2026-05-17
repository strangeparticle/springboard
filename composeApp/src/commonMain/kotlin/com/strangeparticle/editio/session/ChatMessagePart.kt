package com.strangeparticle.editio.session

internal sealed class ChatMessagePart {
    data class UserText(val text: String) : ChatMessagePart()

    data class AssistantText(val text: String) : ChatMessagePart()

    data class ToolCall(
        val toolCall: com.strangeparticle.editio.toolcall.ToolCall,
        val state: ToolCallState,
    ) : ChatMessagePart()

    data class ChatError(val message: String) : ChatMessagePart()
}
