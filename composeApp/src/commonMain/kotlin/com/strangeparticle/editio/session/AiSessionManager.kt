package com.strangeparticle.editio.session

import com.strangeparticle.editio.client.AiProviderClient
import com.strangeparticle.editio.client.AiProviderClientRequest
import com.strangeparticle.editio.conversation.AiConversationMessage
import com.strangeparticle.editio.conversation.AiConversationMessageForAssistant
import com.strangeparticle.editio.conversation.AiConversationMessageForSystemState
import com.strangeparticle.editio.conversation.AiConversationMessageForUser
import com.strangeparticle.editio.session.event.AiChatEvent
import com.strangeparticle.editio.session.event.AssistantErroredAiChatEvent
import com.strangeparticle.editio.session.event.AssistantRespondedAiChatEvent
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
import com.strangeparticle.editio.toolcall.ToolCallDispatcher
import com.strangeparticle.editio.toolcall.ToolCallExecutionResult
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.editio.toolcall.ToolCallRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AiSessionManager(
    private val aiClient: AiProviderClient,
    private val toolCallRegistry: ToolCallRegistry,
    private val snapshotProvider: AiSessionSnapshotProvider,
    private val toolCallExecutionContextFactory: AiSessionToolCallExecutionContextFactory,
    private val systemPromptProvider: () -> String,
    private val modelIdProvider: () -> String,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    /**
     * Approximate token budget for outgoing request history. When the estimated
     * token count exceeds this, the oldest complete turn group is evicted
     * (repeating until under the budget). Estimation uses `text.length / 4`.
     */
    private val maxHistoryTokens: Int = DEFAULT_MAX_HISTORY_TOKENS,
    // TODO: why is done?  function assigned to params can reduce readability
    eventsProvider: (() -> List<AiChatEvent>)? = null,
    appendEvents: ((List<AiChatEvent>) -> Unit)? = null,
    private val onTranscriptChanged: () -> Unit = {},
) {
    private val mutableEvents = mutableListOf<AiChatEvent>()
    private val resolvedEventsProvider: () -> List<AiChatEvent> = eventsProvider ?: { mutableEvents.toList() }
    private val resolvedAppendEvents: (List<AiChatEvent>) -> Unit = appendEvents ?: { mutableEvents += it }
    private val pendingApprovals = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val approvalDecisions = mutableMapOf<String, Boolean>()
    private val toolCallDispatcher = ToolCallDispatcher(toolCallRegistry)

    val events: List<AiChatEvent> get() = resolvedEventsProvider()
    val transcriptParts: List<ChatMessagePart> get() = buildTranscriptParts(events)
    val history: List<AiConversationMessage> get() = buildProviderHistory(events)

    private var currentRequestJob: Job? = null
    var stateChangedSinceLastSnapshotSent = true
        private set

    fun submit(userText: String): Job {
        check(currentRequestJob?.isActive != true) { "An AI request is already in progress." }

        val job = coroutineScope.launch {
            try {
                appendSnapshotIfChanged()
                appendChatEvent(UserSubmittedAiChatEvent(userText))

                runRequestLoop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendChatEvent(AssistantErroredAiChatEvent(e.message ?: "AI request failed"))
            }
        }
        currentRequestJob = job
        return job
    }

    /**
     * Resolve a pending approval for [toolCallId]. UI calls this when the user clicks
     * Apply / Cancel on the inline approval card. Unknown approval ids are ignored;
     * only tools that have already requested approval can be resolved.
     */
    fun onApprovalDecision(toolCallId: String, approved: Boolean) {
        val deferred = pendingApprovals[toolCallId] ?: return
        approvalDecisions[toolCallId] = approved
        appendChatEvent(ToolApprovalRespondedAiChatEvent(toolCallId, approved))
        deferred.complete(approved)
    }

    /**
     * Cancel the in-flight request job (if any). Already-applied tool mutations are
     * NOT rolled back — the user can ask the AI to undo them in a subsequent turn.
     * Pending approvals for this job are cleared so a future submit starts clean.
     */
    fun stop() {
        currentRequestJob?.cancel()
        for (toolCallId in pendingApprovals.keys) {
            appendChatEvent(ToolCallDeniedAiChatEvent(toolCallId))
        }
        pendingApprovals.clear()
        approvalDecisions.clear()
    }

    private suspend fun runRequestLoop() {
        while (true) {
            appendSnapshotIfChanged()
            val requestHistory = evictHistoryIfNeeded(history)
            val response = aiClient.sendAiRequest(
                AiProviderClientRequest(
                    modelId = modelIdProvider(),
                    systemPrompt = systemPromptProvider(),
                    history = requestHistory,
                    tools = toolCallRegistry.getDefinitions(),
                )
            )

            if (response.toolCalls.isEmpty()) {
                response.text?.let { text ->
                    appendChatEvent(AssistantRespondedAiChatEvent(text = text, toolCalls = emptyList()))
                }
                return
            }

            appendChatEvent(AssistantRespondedAiChatEvent(
                text = response.text,
                toolCalls = response.toolCalls,
            ))

            val context = toolCallExecutionContextFactory.createToolCallExecutionContext(
                onStateChanged = { stateChangedSinceLastSnapshotSent = true },
                awaitUserApproval = { toolCallId ->
                    appendChatEvent(ToolApprovalRequestedAiChatEvent(toolCallId))
                    val deferred = pendingApprovals.getOrPut(toolCallId) { CompletableDeferred() }
                    deferred.await()
                },
            )
            for (toolCall in response.toolCalls) {
                appendChatEvent(ToolCallStartedAiChatEvent(toolCall))

                val result = toolCallDispatcher.execute(
                    toolCallId = toolCall.toolCallId,
                    providerToolId = toolCall.toolName,
                    argumentsAsJsonString = toolCall.argumentsAsJsonString,
                    context = context,
                )
                val content = result.toProviderMessageContent()
                pendingApprovals.remove(toolCall.toolCallId)
                val approvalDecision = approvalDecisions.remove(toolCall.toolCallId)
                if (result.endsTurn) {
                    appendChatEvent(ToolCallCompletedAiChatEvent(
                        toolCallId = toolCall.toolCallId,
                        providerContent = content,
                        transcriptOutput = result.toTranscriptOutput(content),
                        endsTurn = true,
                    ))
                    return
                }
                appendToolResultEvent(toolCall.toolCallId, result, content, approvalDecision)
            }
        }
    }

    private fun appendToolResultEvent(
        toolCallId: String,
        result: ToolCallHandlerResponse,
        content: String,
        approvalDecision: Boolean?,
    ) {
        appendChatEvent(when {
            approvalDecision == false -> ToolCallDeniedAiChatEvent(toolCallId)
            result is ToolCallExecutionResult && !result.success -> ToolCallFailedAiChatEvent(
                toolCallId,
                providerContent = content,
                message = result.message ?: result.toTranscriptOutput(content),
            )
            else -> ToolCallCompletedAiChatEvent(
                toolCallId = toolCallId,
                providerContent = content,
                transcriptOutput = result.toTranscriptOutput(content),
                endsTurn = false,
            )
        })
    }

    private fun appendChatEvent(event: AiChatEvent) = appendChatEvents(listOf(event))

    private fun appendChatEvents(events: List<AiChatEvent>) {
        resolvedAppendEvents(events)
        onTranscriptChanged()
    }

    private fun appendSnapshotIfChanged() {
        if (!stateChangedSinceLastSnapshotSent) {
            return
        }
        appendChatEvent(StateSnapshotAddedAiChatEvent(snapshotProvider.getSnapshotJson()))
        stateChangedSinceLastSnapshotSent = false
    }

    /**
     * Evict the oldest complete turn group while estimated history tokens exceed
     * [maxHistoryTokens]. Never evicts the only remaining turn (we always send at
     * least the current turn even if it's individually over budget — that's a
     * provider-side context-length problem, not a history-management problem).
     */
    private fun evictHistoryIfNeeded(history: List<AiConversationMessage>): List<AiConversationMessage> {
        val requestHistory = history.toMutableList()
        while (estimateHistoryTokens(requestHistory) > maxHistoryTokens) {
            val boundaries = turnStartIndices(requestHistory)
            if (boundaries.size <= 1) return requestHistory
            requestHistory.subList(0, boundaries[1]).clear()
        }
        return requestHistory
    }

    private fun estimateHistoryTokens(history: List<AiConversationMessage>): Int = history.sumOf(::estimateMessageTokens)

    private fun estimateMessageTokens(message: AiConversationMessage): Int = when (message) {
        is AiConversationMessageForUser -> estimateTokens(message.text)
        is AiConversationMessageForAssistant -> {
            estimateTokens(message.text ?: "") +
                message.toolCalls.sumOf { estimateTokens(it.toolName) + estimateTokens(it.argumentsAsJsonString) }
        }
        is AiConversationMessageForSystemState -> estimateTokens(message.snapshotJson)
        is ToolCallProviderClientMessage -> estimateTokens(message.content)
        else -> 0
    }

    private fun estimateTokens(text: String): Int = (text.length + 3) / 4

    /**
     * Indices in history where a turn group starts. A turn is anchored on
     * a user message; if that user message is immediately preceded by a snapshot
     * injection (which is the typical "fresh-state" pattern), the snapshot is the
     * start of the turn (it belongs to the user message that follows). Snapshots
     * that appear mid-turn (between tool results and the follow-up assistant
     * response) are part of the surrounding turn, not their own turn.
     */
    private fun turnStartIndices(history: List<AiConversationMessage>): List<Int> {
        val starts = mutableListOf<Int>()
        for (i in history.indices) {
            if (history[i] is AiConversationMessageForUser) {
                val candidate = if (i > 0 && history[i - 1] is AiConversationMessageForSystemState) i - 1 else i
                starts += candidate
            }
        }
        return starts
    }

    companion object {
        /**
         * Default ~75k tokens — about 75% of a 100k context window, leaving room for
         * the system prompt, tool definitions, and the assistant's next response.
         */
        const val DEFAULT_MAX_HISTORY_TOKENS: Int = 75_000
    }
}
