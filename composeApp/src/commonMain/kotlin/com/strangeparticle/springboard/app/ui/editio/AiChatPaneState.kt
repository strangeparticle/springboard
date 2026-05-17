package com.strangeparticle.springboard.app.ui.editio

import com.strangeparticle.editio.session.ChatMessagePart

internal data class AiChatPaneState(
    val isConfigured: Boolean,
    val providerLabel: String,
    val modelLabel: String,
    val transcriptParts: List<ChatMessagePart>,
    val isRunning: Boolean,
    val onSubmit: (String) -> Unit,
    val onStop: () -> Unit,
    val onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
) {
    companion object {
        fun notConfigured(): AiChatPaneState = AiChatPaneState(
            isConfigured = false,
            providerLabel = "Not configured",
            modelLabel = "",
            transcriptParts = emptyList(),
            isRunning = false,
            onSubmit = {},
            onStop = {},
            onApprovalDecision = { _, _ -> },
        )

        fun configured(
            providerLabel: String,
            modelLabel: String,
            transcriptParts: List<ChatMessagePart>,
            isRunning: Boolean = false,
            onSubmit: (String) -> Unit,
            onStop: () -> Unit,
            onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
        ): AiChatPaneState = AiChatPaneState(
            isConfigured = true,
            providerLabel = providerLabel,
            modelLabel = modelLabel,
            transcriptParts = transcriptParts,
            isRunning = isRunning,
            onSubmit = onSubmit,
            onStop = onStop,
            onApprovalDecision = onApprovalDecision,
        )
    }
}
