package com.strangeparticle.springboard.app.ui.editio

import com.strangeparticle.editio.session.ChatMessagePart

internal data class AiChatPaneState(
    val isConfigured: Boolean,
    val providerLabel: String,
    val modelLabel: String,
    val transcriptParts: List<ChatMessagePart>,
    val scrollbackPanes: List<AiChatScrollbackPane>,
    val isRunning: Boolean,
    val onSubmit: (String) -> Unit,
    val onStop: () -> Unit,
    val onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
    val onProcessingFocusFallback: () -> Unit,
) {
    companion object {
        fun notConfigured(): AiChatPaneState = AiChatPaneState(
            isConfigured = false,
            providerLabel = "Not configured",
            modelLabel = "",
            transcriptParts = emptyList(),
            scrollbackPanes = emptyList(),
            isRunning = false,
            onSubmit = {},
            onStop = {},
            onApprovalDecision = { _, _ -> },
            onProcessingFocusFallback = {},
        )

        fun configured(
            providerLabel: String,
            modelLabel: String,
            transcriptParts: List<ChatMessagePart>,
            scrollbackPanes: List<AiChatScrollbackPane> = buildScrollbackPanesFromTranscript(transcriptParts),
            isRunning: Boolean = false,
            onSubmit: (String) -> Unit,
            onStop: () -> Unit,
            onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
            onProcessingFocusFallback: () -> Unit = {},
        ): AiChatPaneState = AiChatPaneState(
            isConfigured = true,
            providerLabel = providerLabel,
            modelLabel = modelLabel,
            transcriptParts = transcriptParts,
            scrollbackPanes = scrollbackPanes,
            isRunning = isRunning,
            onSubmit = onSubmit,
            onStop = onStop,
            onApprovalDecision = onApprovalDecision,
            onProcessingFocusFallback = onProcessingFocusFallback,
        )

        private fun buildScrollbackPanesFromTranscript(transcriptParts: List<ChatMessagePart>): List<AiChatScrollbackPane> {
            val panes = mutableListOf<AiChatScrollbackPane>()
            var currentStartIndex: Int? = null
            var currentRequestText: String? = null
            val currentResponseParts = mutableListOf<ChatMessagePart>()

            fun flush() {
                val requestText = currentRequestText ?: return
                panes += AiChatScrollbackPane.Interaction(
                    requestText = requestText,
                    responseParts = currentResponseParts.toList(),
                    transcriptStartIndex = currentStartIndex,
                )
                currentStartIndex = null
                currentRequestText = null
                currentResponseParts.clear()
            }

            transcriptParts.forEachIndexed { index, part ->
                if (part is ChatMessagePart.UserText) {
                    flush()
                    currentStartIndex = index
                    currentRequestText = part.text
                } else if (currentRequestText != null) {
                    currentResponseParts += part
                }
            }
            flush()
            return panes.ifEmpty {
                if (transcriptParts.isEmpty()) listOf(initialTerseHelpScrollbackPane()) else emptyList()
            }
        }
    }
}
