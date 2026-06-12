package com.strangeparticle.luther.session

import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallRegistry
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun createLutherSession(
    providers: List<AiProvider>,
    settings: LutherSettings,
    httpClient: HttpClient,
    toolHandlers: List<ToolCallHandler>,
    executionContextFactory: AiSessionToolCallExecutionContextFactory,
    snapshotProvider: AiSessionSnapshotProvider,
    systemPromptProvider: () -> String,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
): LutherSession {
    val provider = providers.firstOrNull { it.id == settings.providerId }
        ?: throw IllegalArgumentException("Unknown providerId '${settings.providerId}'")
    require(settings.isComplete(provider)) { "Incomplete LutherSettings for provider '${settings.providerId}'" }
    return LutherSession(
        providers, settings, httpClient, toolHandlers, executionContextFactory,
        snapshotProvider, systemPromptProvider, coroutineScope,
    )
}

internal class LutherSession internal constructor(
    private val providers: List<AiProvider>,
    initialSettings: LutherSettings,
    private val httpClient: HttpClient,
    private val toolHandlers: List<ToolCallHandler>,
    private val executionContextFactory: AiSessionToolCallExecutionContextFactory,
    private val snapshotProvider: AiSessionSnapshotProvider,
    private val systemPromptProvider: () -> String,
    private val coroutineScope: CoroutineScope,
) {
    private val historyState = MutableStateFlow<List<ChatHistoryGroup>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryGroup>> = historyState.asStateFlow()

    private var settings: LutherSettings = initialSettings
    private val statusState = MutableStateFlow(statusFor(initialSettings))
    val status: StateFlow<LutherStatus> = statusState.asStateFlow()

    private var manager: AiSessionManager = buildManager()

    private fun provider(): AiProvider = providers.first { it.id == settings.providerId }

    private fun buildManager(): AiSessionManager {
        val client = provider().createClient(settings.providerConfig, httpClient)
        val registry = ToolCallRegistry().apply { toolHandlers.forEach { register(it) } }
        return AiSessionManager(
            aiClient = client,
            toolCallRegistry = registry,
            snapshotProvider = snapshotProvider,
            toolCallExecutionContextFactory = executionContextFactory,
            systemPromptProvider = systemPromptProvider,
            modelIdProvider = { settings.modelId },
            coroutineScope = coroutineScope,
            groupsProvider = { historyState.value },
            updateGroups = { historyState.value = it },
        )
    }

    fun updateConfiguration(newSettings: LutherSettings) {
        val needsRebuild = newSettings.providerId != settings.providerId ||
            newSettings.providerConfig != settings.providerConfig
        settings = newSettings
        if (needsRebuild) manager = buildManager()
        statusState.value = statusFor(newSettings)
    }

    private fun statusFor(candidateSettings: LutherSettings): LutherStatus {
        val provider = providers.firstOrNull { it.id == candidateSettings.providerId }
        return LutherStatus(
            isReady = provider != null && candidateSettings.isComplete(provider),
            providerId = candidateSettings.providerId,
            modelId = candidateSettings.modelId,
            lastError = null,
        )
    }

    fun submit(userText: String): Job = manager.submit(userText)
    fun stop() = manager.stop()
    fun respondToToolApproval(toolCallId: String, approved: Boolean) =
        manager.onApprovalDecision(toolCallId, approved)
    fun markExternalStateChange() = manager.markExternalStateChange()

    fun close() {
        coroutineScope.cancel()
    }
}
