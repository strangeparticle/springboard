package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.editio.conversation.AiClientMessage
import com.strangeparticle.editio.conversation.AiClientMessageForAssistant
import com.strangeparticle.editio.conversation.AiClientMessageForSystemState
import com.strangeparticle.editio.conversation.AiClientMessageForUser
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.AiSessionManager
import com.strangeparticle.editio.session.AiSessionSnapshotProvider
import com.strangeparticle.editio.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage
import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallRegistry
import com.strangeparticle.springboard.app.editio.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.editio.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.editio.SystemPromptBuilder
import com.strangeparticle.springboard.app.editio.help.AiAssistantFullHelpText
import com.strangeparticle.springboard.app.editio.help.AiAssistantTerseHelpText
import com.strangeparticle.springboard.app.editio.toolcall.*
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.editio.AiChatLocalCommand
import com.strangeparticle.springboard.app.ui.editio.AiChatPaneState
import com.strangeparticle.springboard.app.ui.editio.AiChatScrollbackPane
import com.strangeparticle.springboard.app.ui.editio.CommandAttribution
import com.strangeparticle.springboard.app.ui.editio.LocalCommandResponseStyle
import com.strangeparticle.springboard.app.ui.editio.initialTerseHelpScrollbackPane
import com.strangeparticle.springboard.app.ui.editio.parseAiChatLocalCommand
import com.strangeparticle.springboard.app.ui.settings.ActiveSettingsScreen
import com.strangeparticle.springboard.app.ui.settings.SettingsScreen
import com.strangeparticle.springboard.app.ui.toast.ToastOverlay
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun SpringboardApp(
    viewModel: SpringboardViewModel,
    settingsViewModel: SettingsViewModel,
    showSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
    showActiveSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
    showAssistant: MutableState<Boolean> = remember { mutableStateOf(false) },
    activeSettingsOpenedFromSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
    aiChatPaneState: AiChatPaneState = AiChatPaneState.notConfigured(),
    onOpenSettings: () -> Unit = { showSettings.value = true },
    onOpenActiveSettingsFromSettings: () -> Unit = {
        activeSettingsOpenedFromSettings.value = true
        showActiveSettings.value = true
    },
    onCloseActiveSettings: () -> Unit = {
        showActiveSettings.value = false
        if (!activeSettingsOpenedFromSettings.value) {
            showSettings.value = false
        }
        activeSettingsOpenedFromSettings.value = false
    },
    fileContentService: PlatformFileContentService = PlatformFileContentServiceDefaultImpl(),
    networkContentService: NetworkContentService? = null,
    showFileOpen: Boolean = true,
    aiEnvironmentVariables: Map<String, String> = emptyMap(),
    aiFetchModels: suspend (AiProvider, String) -> List<AiClientModelInfo> = { _, _ -> emptyList() },
    aiClientFactory: (AiProvider, String) -> AiClient? = { _, _ -> null },
) {
    var isShiftHeld by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var aiSettingsFirst by remember { mutableStateOf(false) }
    val derivedAiChatPaneState = rememberAiChatPaneState(
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
        environmentVariables = aiEnvironmentVariables,
        aiClientFactory = aiClientFactory,
        coroutineScope = coroutineScope,
    )
    val effectiveAiChatPaneState = if (aiChatPaneState.isConfigured) aiChatPaneState else derivedAiChatPaneState
    val openAiSettings = {
        aiSettingsFirst = true
        onOpenSettings()
    }

    println("[Springboard] window ready")

    AppTheme(brandId = settingsViewModel.activeBrandId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (showSettings.value) return@onKeyEvent false
                    if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                        if (event.type == KeyEventType.KeyDown) {
                            isShiftHeld = true
                        } else if (event.type == KeyEventType.KeyUp) {
                            isShiftHeld = false
                            if (viewModel.multiSelectSet.isNotEmpty()) {
                                viewModel.activateMultiSelect()
                            }
                        }
                        true
                    } else if (event.type == KeyEventType.KeyDown && event.isMetaPressed && event.isShiftPressed && event.key == Key.A) {
                        showAssistant.value = !showAssistant.value
                        true
                    } else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.isShiftPressed) {
                        when (event.key) {
                            Key.LeftBracket -> { viewModel.selectPreviousTab(); true }
                            Key.RightBracket -> { viewModel.selectNextTab(); true }
                            else -> false
                        }
                    } else false
                }
        ) {
            if (showSettings.value) {
                if (showActiveSettings.value) {
                    ActiveSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = onCloseActiveSettings,
                    )
                } else {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = {
                            aiSettingsFirst = false
                            showSettings.value = false
                        },
                        onShowActiveSettings = onOpenActiveSettingsFromSettings,
                        currentTabSources = viewModel.currentTabSources,
                        aiEnvironmentVariables = aiEnvironmentVariables,
                        aiFetchModels = aiFetchModels,
                        showAiSettingsFirst = aiSettingsFirst,
                    )
                }
            } else {
                MainScreen(
                    viewModel = viewModel,
                    isShiftHeld = isShiftHeld,
                    onOpenSettings = onOpenSettings,
                    fileContentService = fileContentService,
                    networkContentService = networkContentService,
                    showFileOpen = showFileOpen,
                    isAssistantConfigured = effectiveAiChatPaneState.isConfigured,
                    onToggleAssistant = { showAssistant.value = !showAssistant.value },
                    showAssistant = showAssistant.value,
                    aiChatPaneState = effectiveAiChatPaneState,
                    onCloseAssistant = { showAssistant.value = false },
                    onOpenAiSettings = openAiSettings,
                )
            }

            ToastOverlay(
                tabToastState = viewModel.activeTabToast,
                isTabVisible = !showSettings.value,
                onToastDismissed = {
                    viewModel.requestFocusAppDropdown()
                },
            )
        }
    }
}

@Composable
private fun rememberAiChatPaneState(
    viewModel: SpringboardViewModel,
    settingsViewModel: SettingsViewModel,
    environmentVariables: Map<String, String>,
    aiClientFactory: (AiProvider, String) -> AiClient?,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): AiChatPaneState {
    settingsViewModel.settingsVersion
    val provider = AiProvider.fromId(
        settingsViewModel.getResolvedValue(SettingsKey.AI_PROVIDER) as? String ?: AiProvider.None.id,
    )
    val apiKey = resolveAiApiKey(provider, settingsViewModel, environmentVariables)
    val modelId = settingsViewModel.getResolvedValue(SettingsKey.AI_MODEL) as? String ?: ""
    val aiClient = remember(provider, apiKey) {
        apiKey?.let { aiClientFactory(provider, it) }
    }
    var transcriptVersion by remember { mutableStateOf(0) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var scrollbackPanes by remember(aiClient, modelId, viewModel) {
        mutableStateOf(listOf<AiChatScrollbackPane>(initialTerseHelpScrollbackPane()))
    }

    if (provider == AiProvider.None || apiKey == null || modelId.isBlank() || aiClient == null) {
        return AiChatPaneState.notConfigured()
    }

    val manager = remember(aiClient, modelId, viewModel) {
        AiSessionManager(
            aiClient = aiClient,
            toolCallRegistry = createSpringboardToolCallRegistry(),
            snapshotProvider = object : AiSessionSnapshotProvider {
                override fun getSnapshotJson(): String = SpringboardAppSnapshot.capture(viewModel).toCompactJson()
            },
            toolCallExecutionContextFactory = object : AiSessionToolCallExecutionContextFactory {
                override fun createToolCallExecutionContext(
                    onStateChanged: () -> Unit,
                    awaitUserApproval: suspend (toolCallId: String) -> Boolean,
                ): ToolCallExecutionContext = object : SpringboardToolCallExecutionContext {
                    override val viewModel: SpringboardViewModel = viewModel
                    override fun markStateChanged() = onStateChanged()
                    override suspend fun awaitUserApproval(toolCallId: String): Boolean = awaitUserApproval(toolCallId)
                }
            },
            systemPromptProvider = { SystemPromptBuilder.build() },
            modelIdProvider = { modelId },
            coroutineScope = coroutineScope,
            onTranscriptChanged = { transcriptVersion++ },
        )
    }
    transcriptVersion
    val showFullChatTranscript =
        (settingsViewModel.getResolvedValue(SettingsKey.SHOW_FULL_CHAT_TRANSCRIPT) as? Boolean) == true
    val effectiveScrollbackPanes = if (showFullChatTranscript) {
        buildDebugScrollbackPanes(manager.history)
    } else {
        scrollbackPanes = syncAiTranscriptPanes(scrollbackPanes, manager.transcriptParts)
        scrollbackPanes
    }

    return AiChatPaneState.configured(
        providerLabel = provider.displayName,
        modelLabel = modelId,
        transcriptParts = manager.transcriptParts,
        scrollbackPanes = effectiveScrollbackPanes,
        isRunning = runningJob?.isActive == true,
        onSubmit = { text ->
            when (val command = parseAiChatLocalCommand(text)) {
                is AiChatLocalCommand.HelpTerse -> {
                    scrollbackPanes = scrollbackPanes + AiChatScrollbackPane.LocalCommand(
                        commandText = command.originalText,
                        commandAttribution = CommandAttribution.User,
                        responseText = AiAssistantTerseHelpText.text,
                        style = LocalCommandResponseStyle.Help,
                    )
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.HelpFull -> {
                    scrollbackPanes = scrollbackPanes + AiChatScrollbackPane.LocalCommand(
                        commandText = command.originalText,
                        commandAttribution = CommandAttribution.User,
                        responseText = AiAssistantFullHelpText.text,
                        style = LocalCommandResponseStyle.Help,
                    )
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.Unknown -> {
                    scrollbackPanes = scrollbackPanes + AiChatScrollbackPane.LocalCommand(
                        commandText = command.originalText,
                        commandAttribution = CommandAttribution.User,
                        responseText = "Unknown command: ${command.originalText}. Try /help.",
                        style = LocalCommandResponseStyle.Error,
                    )
                    transcriptVersion++
                    return@configured
                }
                null -> Unit
            }
            val job = manager.submit(text)
            runningJob = job
            transcriptVersion++
            coroutineScope.launch {
                job.join()
                runningJob = null
                transcriptVersion++
            }
        },
        onStop = {
            manager.stop()
            runningJob = null
            transcriptVersion++
        },
        onApprovalDecision = { toolCallId, approved ->
            manager.onApprovalDecision(toolCallId, approved)
            transcriptVersion++
        },
    )
}

private fun syncAiTranscriptPanes(
    scrollbackPanes: List<AiChatScrollbackPane>,
    transcriptParts: List<ChatMessagePart>,
): List<AiChatScrollbackPane> {
    val aiPanes = buildAiInteractionPanes(transcriptParts)
    var synced = scrollbackPanes
    for (aiPane in aiPanes) {
        val existingIndex = synced.indexOfFirst {
            it is AiChatScrollbackPane.Interaction && it.transcriptStartIndex == aiPane.transcriptStartIndex
        }
        synced = if (existingIndex >= 0) {
            synced.toMutableList().also { it[existingIndex] = aiPane }
        } else {
            synced + aiPane
        }
    }
    return synced.ifEmpty { listOf(initialTerseHelpScrollbackPane()) }
}

/**
 * Build the chat pane list from the provider-side history when the developer-tools
 * "Show Full Chat Transcript" setting is on. Each [AiClientMessage] becomes its own
 * pane so snapshots and raw tool-result payloads (which the normal user-facing
 * view hides) are visible and copy-pasteable individually.
 */
private fun buildDebugScrollbackPanes(history: List<AiClientMessage>): List<AiChatScrollbackPane> =
    history.mapIndexedNotNull { index, message ->
        when (message) {
            is AiClientMessageForUser -> AiChatScrollbackPane.DebugUserMessage(
                text = message.text,
                historyIndex = index,
            )
            is AiClientMessageForSystemState -> AiChatScrollbackPane.DebugStateSnapshot(
                snapshotJson = message.snapshotJson,
                historyIndex = index,
            )
            is AiClientMessageForAssistant -> AiChatScrollbackPane.DebugAssistantMessage(
                text = message.text,
                toolCalls = message.toolCalls,
                historyIndex = index,
            )
            is ToolCallProviderClientMessage -> AiChatScrollbackPane.DebugToolResult(
                toolCallId = message.toolCallId,
                content = message.content,
                historyIndex = index,
            )
            else -> null
        }
    }

private fun buildAiInteractionPanes(transcriptParts: List<ChatMessagePart>): List<AiChatScrollbackPane.Interaction> {
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

private fun resolveAiApiKey(
    provider: AiProvider,
    settingsViewModel: SettingsViewModel,
    environmentVariables: Map<String, String>,
): String? {
    val envName = when (provider) {
        AiProvider.OpenAi -> "OPENAI_API_KEY"
        AiProvider.Anthropic -> "ANTHROPIC_API_KEY"
        AiProvider.None -> return null
    }
    environmentVariables[envName]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val key = when (provider) {
        AiProvider.OpenAi -> SettingsKey.AI_OPENAI_API_KEY
        AiProvider.Anthropic -> SettingsKey.AI_ANTHROPIC_API_KEY
        AiProvider.None -> return null
    }
    return (settingsViewModel.getResolvedValue(key) as? String)?.trim()?.takeIf { it.isNotEmpty() }
}

private fun createSpringboardToolCallRegistry(): ToolCallRegistry = ToolCallRegistry().apply {
    register(AddAppGroupToolCallHandler())
    register(AddAppToolCallHandler())
    register(AddCommandActivatorToolCallHandler())
    register(AddEnvironmentToolCallHandler())
    register(AddGuidanceToolCallHandler())
    register(AddResourceToolCallHandler())
    register(AddUrlActivatorToolCallHandler())
    register(AddUrlTemplateActivatorToolCallHandler())
    register(CloseTabToolCallHandler())
    register(CreateSpringboardToolCallHandler())
    register(CreateTabToolCallHandler())
    register(MoveActivatorToolCallHandler())
    register(OpenFromUrlToolCallHandler())
    register(OpenLocalFileToolCallHandler())
    register(RemoveActivatorToolCallHandler())
    register(RemoveAppGroupToolCallHandler())
    register(RemoveAppToolCallHandler())
    register(RemoveEnvironmentToolCallHandler())
    register(RemoveGuidanceToolCallHandler())
    register(RemoveResourceToolCallHandler())
    register(ReorderActivatorsToolCallHandler())
    register(ReorderAppGroupsToolCallHandler())
    register(ReorderAppsToolCallHandler())
    register(ReorderEnvironmentsToolCallHandler())
    register(ReorderResourcesToolCallHandler())
    register(RespondWithMessageToolCallHandler())
    register(SaveSpringboardToolCallHandler())
    register(UpdateActivatorToolCallHandler())
    register(UpdateAppGroupToolCallHandler())
    register(UpdateAppToolCallHandler())
    register(UpdateEnvironmentToolCallHandler())
    register(UpdateGuidanceToolCallHandler())
    register(UpdateResourceToolCallHandler())
}
