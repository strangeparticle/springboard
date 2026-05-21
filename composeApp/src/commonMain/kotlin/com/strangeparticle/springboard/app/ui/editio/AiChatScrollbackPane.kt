package com.strangeparticle.springboard.app.ui.editio

import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.editio.session.event.AiChatEvent
import com.strangeparticle.editio.session.event.AssistantErroredAiChatEvent
import com.strangeparticle.editio.session.event.AssistantRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandResponseKind
import com.strangeparticle.editio.session.event.LocalCommandSource
import com.strangeparticle.editio.session.event.LocalCommandSubmittedAiChatEvent
import com.strangeparticle.editio.session.event.StateSnapshotAddedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRequestedAiChatEvent
import com.strangeparticle.editio.session.event.ToolApprovalRespondedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallCompletedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallDeniedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallFailedAiChatEvent
import com.strangeparticle.editio.session.event.ToolCallStartedAiChatEvent
import com.strangeparticle.editio.session.event.UserSubmittedAiChatEvent
import com.strangeparticle.editio.session.projection.buildProviderHistory
import com.strangeparticle.editio.session.projection.buildTranscriptParts
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.springboard.app.editio.help.AiAssistantTerseHelpText

internal sealed class AiChatScrollbackPane {
    data class LocalCommand(
        val commandText: String,
        val commandAttribution: CommandAttribution,
        val responseText: String,
        val style: LocalCommandResponseStyle,
    ) : AiChatScrollbackPane()

    data class Interaction(
        val requestText: String,
        val responseParts: List<ChatMessagePart>,
        val transcriptStartIndex: Int? = null,
    ) : AiChatScrollbackPane()

    // Debug-only panes used when SHOW_FULL_CHAT_TRANSCRIPT is on. Each represents
    // exactly one AiClientMessage from AiSessionManager.history so the developer
    // can see every payload exchanged with the model — including the state
    // snapshots and raw tool-result payloads the normal Interaction view hides.

    data class DebugUserMessage(
        val text: String,
        val historyIndex: Int,
    ) : AiChatScrollbackPane()

    data class DebugStateSnapshot(
        val snapshotJson: String,
        val historyIndex: Int,
    ) : AiChatScrollbackPane()

    data class DebugAssistantMessage(
        val text: String?,
        val toolCalls: List<ToolCall>,
        val historyIndex: Int,
    ) : AiChatScrollbackPane()

    data class DebugToolResult(
        val toolCallId: String,
        val content: String,
        val historyIndex: Int,
    ) : AiChatScrollbackPane()
}

internal enum class CommandAttribution {
    System,
    User,
}

internal enum class LocalCommandResponseStyle {
    Help,
    Error,
}

internal fun initialTerseHelpScrollbackPane(): AiChatScrollbackPane.LocalCommand = AiChatScrollbackPane.LocalCommand(
    commandText = "/help_terse",
    commandAttribution = CommandAttribution.System,
    responseText = AiAssistantTerseHelpText.text,
    style = LocalCommandResponseStyle.Help,
)

internal fun initialTerseHelpEvents(): List<AiChatEvent> = listOf(
    LocalCommandSubmittedAiChatEvent("/help_terse", LocalCommandSource.System),
    LocalCommandRespondedAiChatEvent("/help_terse", AiAssistantTerseHelpText.text, LocalCommandResponseKind.Help),
)

internal fun buildSlimScrollbackPanes(events: List<AiChatEvent>): List<AiChatScrollbackPane> {
    val panes = mutableListOf<AiChatScrollbackPane>()
    val transcriptParts = buildTranscriptParts(events)
    val aiPanes = buildAiInteractionPanes(transcriptParts)
    var aiPaneIndex = 0
    var pendingCommand: LocalCommandSubmittedAiChatEvent? = null

    for (event in events) {
        when (event) {
            is LocalCommandSubmittedAiChatEvent -> pendingCommand = event
            is LocalCommandRespondedAiChatEvent -> {
                val submitted = pendingCommand?.takeIf { it.commandText == event.commandText }
                panes += AiChatScrollbackPane.LocalCommand(
                    commandText = event.commandText,
                    commandAttribution = submitted?.source.toCommandAttribution(),
                    responseText = event.responseText,
                    style = event.kind.toLocalCommandResponseStyle(),
                )
                pendingCommand = null
            }
            is UserSubmittedAiChatEvent -> panes += aiPanes[aiPaneIndex++]
            is AssistantErroredAiChatEvent,
            is AssistantRespondedAiChatEvent,
            is StateSnapshotAddedAiChatEvent,
            is ToolCallCompletedAiChatEvent,
            is ToolCallFailedAiChatEvent,
            is ToolApprovalRequestedAiChatEvent,
            is ToolApprovalRespondedAiChatEvent,
            is ToolCallDeniedAiChatEvent,
            is ToolCallStartedAiChatEvent -> Unit
        }
    }
    return panes.ifEmpty { listOf(initialTerseHelpScrollbackPane()) }
}

internal fun buildDebugScrollbackPanes(events: List<AiChatEvent>): List<AiChatScrollbackPane> =
    buildProviderHistory(events).mapIndexedNotNull { index, message ->
        when (message) {
            is AiClientMessageForUser -> AiChatScrollbackPane.DebugUserMessage(message.text, index)
            is AiClientMessageForSystemState -> AiChatScrollbackPane.DebugStateSnapshot(message.snapshotJson, index)
            is AiClientMessageForAssistant -> AiChatScrollbackPane.DebugAssistantMessage(message.text, message.toolCalls, index)
            is ToolCallProviderClientMessage -> AiChatScrollbackPane.DebugToolResult(message.toolCallId, message.content, index)
            else -> null
        }
    }

internal fun buildAiInteractionPanes(transcriptParts: List<ChatMessagePart>): List<AiChatScrollbackPane.Interaction> {
    val panes = mutableListOf<AiChatScrollbackPane.Interaction>()
    var startIndex: Int? = null
    var requestText: String? = null
    val responseParts = mutableListOf<ChatMessagePart>()

    fun flush() {
        val request = requestText ?: return
        panes += AiChatScrollbackPane.Interaction(
            requestText = request,
            responseParts = responseParts.toList(),
            transcriptStartIndex = startIndex,
        )
        startIndex = null
        requestText = null
        responseParts.clear()
    }

    transcriptParts.forEachIndexed { index, part ->
        if (part is ChatMessagePart.UserText) {
            flush()
            startIndex = index
            requestText = part.text
        } else if (requestText != null) {
            responseParts += part
        }
    }
    flush()
    return panes
}

private fun LocalCommandSource?.toCommandAttribution(): CommandAttribution = when (this) {
    LocalCommandSource.System -> CommandAttribution.System
    LocalCommandSource.User,
    null -> CommandAttribution.User
}

private fun LocalCommandResponseKind.toLocalCommandResponseStyle(): LocalCommandResponseStyle = when (this) {
    LocalCommandResponseKind.Help -> LocalCommandResponseStyle.Help
    LocalCommandResponseKind.Error -> LocalCommandResponseStyle.Error
}

/**
 * Plain-English title shown both at the top of each debug pane (UI) and as the
 * first line of its copy-to-clipboard payload. Kept in one place so the two
 * stay in sync.
 */
/**
 * Plain-English title shown both at the top of each debug pane (UI) and as the
 * first line of its copy-to-clipboard payload. Kept in one place so the two
 * stay in sync. Non-debug pane variants don't have a title bar — they pre-date
 * this view and label themselves inline.
 */
internal fun debugPaneTitle(pane: AiChatScrollbackPane): String = when (pane) {
    is AiChatScrollbackPane.DebugUserMessage -> "Your message"
    is AiChatScrollbackPane.DebugStateSnapshot -> "Application state sent to model"
    is AiChatScrollbackPane.DebugAssistantMessage -> "Assistant reply"
    is AiChatScrollbackPane.DebugToolResult -> "Tool result returned to model — id=${pane.toolCallId}"
    is AiChatScrollbackPane.LocalCommand,
    is AiChatScrollbackPane.Interaction -> ""
}

internal fun getScrollbackPaneTextForCopyToClipboard(pane: AiChatScrollbackPane): String = when (pane) {
    is AiChatScrollbackPane.LocalCommand -> listOf(
        formatCommandForCopyToClipboard(pane),
        pane.responseText,
    ).joinToString("\n\n")
    is AiChatScrollbackPane.Interaction -> listOf(
        "You: ${pane.requestText}",
        pane.responseParts.flatMap(::formatAiResponsePartForCopyToClipboard).joinToString("\n"),
    ).filter { it.isNotBlank() }.joinToString("\n\n")
    is AiChatScrollbackPane.DebugUserMessage -> "${debugPaneTitle(pane)}:\n${pane.text}"
    is AiChatScrollbackPane.DebugStateSnapshot -> "${debugPaneTitle(pane)}:\n${pane.snapshotJson}"
    is AiChatScrollbackPane.DebugAssistantMessage -> buildString {
        append(debugPaneTitle(pane))
        append(":")
        if (!pane.text.isNullOrEmpty()) {
            append("\n")
            append(pane.text)
        }
        for (toolCall in pane.toolCalls) {
            append("\n\nTool call: ${toolCall.toolName}\n")
            append(toolCall.argumentsAsJsonString)
        }
    }
    is AiChatScrollbackPane.DebugToolResult -> "${debugPaneTitle(pane)}:\n${pane.content}"
}

internal fun getAllScrollbackTextForCopyToClipboard(panes: List<AiChatScrollbackPane>): String = panes
    .map(::getScrollbackPaneTextForCopyToClipboard)
    .filter { it.isNotBlank() }
    .joinToString("\n\n")

private fun formatCommandForCopyToClipboard(pane: AiChatScrollbackPane.LocalCommand): String = when (pane.commandAttribution) {
    CommandAttribution.System -> pane.commandText
    CommandAttribution.User -> "You: ${pane.commandText}"
}

internal fun formatAiResponsePartForCopyToClipboard(part: ChatMessagePart): List<String> = when (part) {
    is ChatMessagePart.UserText -> listOf("You: ${part.text}")
    is ChatMessagePart.AssistantText -> listOf("Assistant: ${part.text}")
    is ChatMessagePart.ChatError -> listOf("Error: ${part.message}")
    is ChatMessagePart.ToolCall -> formatToolCallForCopyToClipboard(part)
}

internal fun formatToolCallForCopyToClipboard(part: ChatMessagePart.ToolCall): List<String> = when (val state = part.state) {
    ToolCallState.Pending -> listOf("Working")
    ToolCallState.ApprovalRequested -> listOf("Approval requested")
    is ToolCallState.ApprovalResponded -> listOf(if (state.approved) "Approval granted" else "Approval denied")
    is ToolCallState.OutputAvailable -> emptyList()
    is ToolCallState.OutputError -> listOf("Error: ${state.message}")
    ToolCallState.OutputDenied -> listOf("Denied")
}
