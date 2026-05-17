package com.strangeparticle.editio.session

import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.AiClientRequest
import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.toolcall.ToolCallDispatcher
import com.strangeparticle.editio.toolcall.ToolCallExecutionResult
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.editio.toolcall.ToolCallRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AiSessionManager(
    private val aiClient: AiClient,
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
) {
    private val mutableTranscriptParts = mutableListOf<ChatMessagePart>()
    private val mutableHistory = mutableListOf<AiClientMessage>()
    private val pendingApprovals = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val approvalDecisions = mutableMapOf<String, Boolean>()
    private val toolCallDispatcher = ToolCallDispatcher(toolCallRegistry)

    val transcriptParts: List<ChatMessagePart> get() = mutableTranscriptParts.toList()
    val history: List<AiClientMessage> get() = mutableHistory.toList()

    private var currentRequestJob: Job? = null
    var stateChangedSinceLastSnapshotSent = true
        private set

    fun submit(userText: String): Job {
        check(currentRequestJob?.isActive != true) { "An AI request is already in progress." }
        mutableTranscriptParts += ChatMessagePart.UserText(userText)

        val job = coroutineScope.launch {
            appendSnapshotIfChanged()
            mutableHistory += AiClientMessageForUser(userText)

            runRequestLoop()
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
        transitionToolCallStateTo(toolCallId, ToolCallState.ApprovalResponded(approved))
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
            transitionToolCallStateTo(toolCallId, ToolCallState.OutputDenied)
        }
        pendingApprovals.clear()
        approvalDecisions.clear()
    }

    private suspend fun runRequestLoop() {
        while (true) {
            appendSnapshotIfChanged()
            evictHistoryIfNeeded()
            val response = aiClient.sendAiRequest(
                AiClientRequest(
                    modelId = modelIdProvider(),
                    systemPrompt = systemPromptProvider(),
                    history = mutableHistory.toList(),
                    tools = toolCallRegistry.getDefinitions(),
                )
            )

            if (response.toolCalls.isEmpty()) {
                response.text?.let { text ->
                    mutableTranscriptParts += ChatMessagePart.AssistantText(text)
                    mutableHistory += AiClientMessageForAssistant(text = text)
                }
                return
            }

            mutableHistory += AiClientMessageForAssistant(
                text = response.text,
                toolCalls = response.toolCalls,
            )

            val context = toolCallExecutionContextFactory.createToolCallExecutionContext(
                onStateChanged = { stateChangedSinceLastSnapshotSent = true },
                awaitUserApproval = { toolCallId ->
                    transitionToolCallStateTo(toolCallId, ToolCallState.ApprovalRequested)
                    val deferred = pendingApprovals.getOrPut(toolCallId) { CompletableDeferred() }
                    deferred.await()
                },
            )
            for (toolCall in response.toolCalls) {
                val transcriptIndex = mutableTranscriptParts.size
                mutableTranscriptParts += ChatMessagePart.ToolCall(toolCall, ToolCallState.Pending)

                val result = toolCallDispatcher.execute(
                    toolCallId = toolCall.toolCallId,
                    providerToolId = toolCall.toolName,
                    argumentsAsJsonString = toolCall.argumentsAsJsonString,
                    context = context,
                )
                val content = result.toProviderMessageContent()
                mutableHistory += ToolCallProviderClientMessage(
                    toolCallId = toolCall.toolCallId,
                    content = content,
                )
                pendingApprovals.remove(toolCall.toolCallId)
                val approvalDecision = approvalDecisions.remove(toolCall.toolCallId)
                mutableTranscriptParts[transcriptIndex] = ChatMessagePart.ToolCall(
                    toolCall = toolCall,
                    state = result.toToolCallState(content, approvalDecision),
                )
            }
        }
    }

    private fun transitionToolCallStateTo(toolCallId: String, newState: ToolCallState) {
        val index = mutableTranscriptParts.indexOfLast {
            it is ChatMessagePart.ToolCall && it.toolCall.toolCallId == toolCallId
        }
        if (index < 0) return
        val existing = mutableTranscriptParts[index] as ChatMessagePart.ToolCall
        mutableTranscriptParts[index] = existing.copy(state = newState)
    }

    private fun appendSnapshotIfChanged() {
        if (!stateChangedSinceLastSnapshotSent) {
            return
        }
        mutableHistory += AiClientMessageForSystemState(snapshotProvider.getSnapshotJson())
        stateChangedSinceLastSnapshotSent = false
    }

    private fun Any.toToolCallState(content: String, approvalDecision: Boolean?): ToolCallState =
        when {
            approvalDecision == false -> ToolCallState.OutputDenied
            this is ToolCallExecutionResult && !success -> ToolCallState.OutputError(message ?: content)
            else -> ToolCallState.OutputAvailable(content)
        }

    /**
     * Evict the oldest complete turn group while estimated history tokens exceed
     * [maxHistoryTokens]. Never evicts the only remaining turn (we always send at
     * least the current turn even if it's individually over budget — that's a
     * provider-side context-length problem, not a history-management problem).
     */
    private fun evictHistoryIfNeeded() {
        while (estimateHistoryTokens() > maxHistoryTokens) {
            val boundaries = turnStartIndices()
            if (boundaries.size <= 1) return
            mutableHistory.subList(0, boundaries[1]).clear()
        }
    }

    private fun estimateHistoryTokens(): Int = mutableHistory.sumOf(::estimateMessageTokens)

    private fun estimateMessageTokens(message: AiClientMessage): Int = when (message) {
        is AiClientMessageForUser -> estimateTokens(message.text)
        is AiClientMessageForAssistant -> {
            estimateTokens(message.text ?: "") +
                message.toolCalls.sumOf { estimateTokens(it.toolName) + estimateTokens(it.argumentsAsJsonString) }
        }
        is AiClientMessageForSystemState -> estimateTokens(message.snapshotJson)
        is ToolCallProviderClientMessage -> estimateTokens(message.content)
        else -> 0
    }

    private fun estimateTokens(text: String): Int = (text.length + 3) / 4

    /**
     * Indices in [mutableHistory] where a turn group starts. A turn is anchored on
     * a user message; if that user message is immediately preceded by a snapshot
     * injection (which is the typical "fresh-state" pattern), the snapshot is the
     * start of the turn (it belongs to the user message that follows). Snapshots
     * that appear mid-turn (between tool results and the follow-up assistant
     * response) are part of the surrounding turn, not their own turn.
     */
    private fun turnStartIndices(): List<Int> {
        val starts = mutableListOf<Int>()
        for (i in mutableHistory.indices) {
            if (mutableHistory[i] is AiClientMessageForUser) {
                val candidate = if (i > 0 && mutableHistory[i - 1] is AiClientMessageForSystemState) i - 1 else i
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
