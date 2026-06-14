package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalWindowInfo
import com.strangeparticle.luther.core.client.provider.AiProvider
import com.strangeparticle.luther.core.client.provider.ChatRequest
import com.strangeparticle.luther.core.client.provider.ChatResponse
import com.strangeparticle.luther.core.client.provider.LutherBuiltInProviders
import com.strangeparticle.luther.core.client.provider.LutherProviderCatalog
import com.strangeparticle.luther.core.session.AiSessionManager
import com.strangeparticle.luther.core.session.AiSessionSnapshotProvider
import com.strangeparticle.luther.core.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.luther.core.session.ChatHistoryGroup
import com.strangeparticle.luther.core.session.ChatHistoryGroupType
import com.strangeparticle.luther.core.session.buildChatHistoryDebugDumpJson
import com.strangeparticle.luther.core.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.luther.core.session.event.LocalCommandResponseKind
import com.strangeparticle.luther.core.session.event.LocalCommandSource
import com.strangeparticle.luther.core.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.luther.core.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.core.toolcall.ToolCallExecutionResult
import com.strangeparticle.luther.core.toolcall.ToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.provider.AiProviderSettingsAdaptorRegistry
import com.strangeparticle.springboard.app.luther.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.luther.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.SystemPromptBuilder
import com.strangeparticle.springboard.app.luther.help.AiAssistantFullHelpText
import com.strangeparticle.springboard.app.luther.help.AiAssistantTerseHelpText
import com.strangeparticle.springboard.app.luther.toolcall.*
import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.luther.AiChatLocalCommand
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneModelPickerState
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneState
import com.strangeparticle.springboard.app.ui.luther.buildDebugScrollbackPanes
import com.strangeparticle.springboard.app.ui.luther.buildSlimScrollbackPanes
import com.strangeparticle.luther.core.session.appendProviderModelState
import com.strangeparticle.luther.core.session.initialChatHistory
import com.strangeparticle.luther.core.session.localCommandGroup
import com.strangeparticle.springboard.app.ui.luther.parseAiChatLocalCommand
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
    // Single switch for the entire AI assistant feature (toggle, chat pane, settings). A
    // deployment that cannot support the assistant sets this to false; everything else is gated
    // off it. Defaults to true so stock behavior is unchanged.
    aiAssistantEnabled: Boolean = true,
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
    openFileDialog: () -> String? = { com.strangeparticle.springboard.app.platform.openFileDialog(null) },
    undoRedoBridge: UndoRedoMenuBridge = remember { UndoRedoMenuBridge() },
) {
    var isShiftHeld by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Shift state is only cleared by a Shift KeyUp, which never arrives if focus leaves the window
    // while Shift is held (e.g. Cmd+Tab). Reset it on blur so the grid does not get stuck in
    // multi-select mode, and discard any pending multi-select that was being built.
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (!windowInfo.isWindowFocused) {
            isShiftHeld = false
            viewModel.clearMultiSelect()
        }
    }

    var aiSettingsFirst by remember { mutableStateOf(false) }
    val derivedAiChatPaneState = rememberAiChatPaneState(
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
        coroutineScope = coroutineScope,
        undoRedoBridge = undoRedoBridge,
    )
    // When the assistant is disabled, present an unconfigured pane state so no provider
    // resolution or model-option loading is observed by the UI, regardless of user settings.
    val effectiveAiChatPaneState = if (!aiAssistantEnabled) {
        AiChatPaneState.notConfigured()
    } else if (aiChatPaneState.isConfigured) {
        aiChatPaneState
    } else {
        derivedAiChatPaneState
    }
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
                    } else if (aiAssistantEnabled && event.type == KeyEventType.KeyDown && event.isMetaPressed && event.isShiftPressed && event.key == Key.A) {
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
                            viewModel.requestFocusAppDropdown()
                        },
                        onShowActiveSettings = onOpenActiveSettingsFromSettings,
                        currentTabSources = viewModel.currentTabSources,
                        showAiSettingsFirst = aiSettingsFirst,
                        aiAssistantEnabled = aiAssistantEnabled,
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
                    openFileDialog = openFileDialog,
                    isAssistantConfigured = effectiveAiChatPaneState.isConfigured,
                    onToggleAssistant = { showAssistant.value = !showAssistant.value },
                    showAssistant = showAssistant.value,
                    aiAssistantEnabled = aiAssistantEnabled,
                    aiChatPaneState = effectiveAiChatPaneState.copy(focusInputOnShow = showAssistant.value),
                    onCloseAssistant = {
                        showAssistant.value = false
                        viewModel.requestFocusAppDropdown()
                    },
                    onOpenAiSettings = openAiSettings,
                )
            }

            ToastOverlay(
                tabToastState = viewModel.activeTabToast,
                isTabVisible = !showSettings.value,
                onToastDismissed = {
                    if (!showAssistant.value) viewModel.requestFocusAppDropdown()
                },
            )
        }
    }
}

@Composable
private fun rememberAiChatPaneState(
    viewModel: SpringboardViewModel,
    settingsViewModel: SettingsViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    undoRedoBridge: UndoRedoMenuBridge,
): AiChatPaneState {
    settingsViewModel.settingsVersion
    val selectedProviderId = settingsViewModel.getResolvedValue(AiProviderSetting)
    val httpClient = settingsViewModel.aiHttpClient
    val builtInProviders = remember(httpClient) { LutherBuiltInProviders.all(httpClient) }
    val provider: AiProvider? = builtInProviders.firstOrNull { it.id == selectedProviderId }
    val adaptor = AiProviderSettingsAdaptorRegistry.byId(selectedProviderId)
    val providerConfig = adaptor?.buildProviderConfig(settingsViewModel)
    val isConfigured = provider != null && providerConfig != null && provider.isConfigured(providerConfig)
    val modelId = adaptor?.let { settingsViewModel.getResolvedValue(it.preferredModelSetting) }.orEmpty()
    var modelOptionsResult by remember(selectedProviderId) { mutableStateOf<Result<List<DropDownOption>>?>(null) }
    var isModelOptionsLoading by remember(selectedProviderId) { mutableStateOf(false) }

    val catalog = remember(builtInProviders) { LutherProviderCatalog(builtInProviders) }

    fun loadModelOptions() {
        val activeConfig = providerConfig ?: return
        coroutineScope.launch {
            isModelOptionsLoading = true
            modelOptionsResult = runCatching {
                catalog.availableModels(selectedProviderId, activeConfig)
                    .map { DropDownOption(it.valueId, it.displayLabel) }
            }
            isModelOptionsLoading = false
        }
    }
    LaunchedEffect(selectedProviderId, isConfigured, settingsViewModel.settingsVersion) {
        if (providerConfig != null && isConfigured) loadModelOptions()
    }

    val sendChat: (suspend (ChatRequest) -> ChatResponse)? = remember(provider, isConfigured, providerConfig) {
        if (provider != null && providerConfig != null && provider.isConfigured(providerConfig)) {
            { request -> provider.sendChat(providerConfig, request) }
        } else {
            null
        }
    }
    var transcriptVersion by remember { mutableStateOf(0) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    // TODO: what's this change doing here?  Seems like this should be part of a different commit?
    val latestModelId by rememberUpdatedState(modelId)
    var chatHistory by remember(viewModel) {
        mutableStateOf<List<ChatHistoryGroup>>(emptyList())
    }

    if (provider == null || sendChat == null || modelId.isBlank()) {
        // The assistant isn't configured, so there's no undo engine wired up. Disable the desktop
        // Edit menu's Undo/Redo items so they don't appear actionable.
        SideEffect {
            undoRedoBridge.canUndo = false
            undoRedoBridge.canRedo = false
            undoRedoBridge.onUndo = {}
            undoRedoBridge.onRedo = {}
        }
        return AiChatPaneState.notConfigured()
    }
    val providerLabel = provider.displayName
    // Record the active provider/model as a history entry. Keying the effect on the effective
    // provider:model means it runs once when a configured provider/model first appears and again
    // only when that value actually changes — not on every recomposition. The first record starts
    // the history (provider/model entry, then terse help); later changes append a change entry.
    val activeProviderModelDisplayString = "$providerLabel:$modelId"
    LaunchedEffect(activeProviderModelDisplayString) {
        chatHistory = if (chatHistory.isEmpty()) {
            initialChatHistory(providerLabel, modelId, AiAssistantTerseHelpText.text)
        } else {
            appendProviderModelState(chatHistory, providerLabel, modelId)
        }
    }

    val manager = remember(sendChat, viewModel) {
        AiSessionManager(
            sendChat = sendChat,
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
            modelIdProvider = { latestModelId },
            coroutineScope = coroutineScope,
            groupsProvider = { chatHistory },
            updateGroups = { groups -> chatHistory = groups },
            onTranscriptChanged = { transcriptVersion++ },
            onTurnStart = { viewModel.beginEditTransaction() },
            onTurnEnd = { viewModel.commitEditTransaction() },
        )
    }
    transcriptVersion

    // `/undo` and `/redo` are chat commands that dispatch the registered `undo`/`redo` tool-calls
    // (the same handlers the assistant and external agents use), then append a visible chat event.
    // Undo-history and chat-history are separate concerns: these commands never delete a chat item.
    // The tool marks state changed through the context's onStateChanged, so no extra
    // markExternalStateChange() is needed here.
    val performUndo: () -> Unit = {
        if (runningJob?.isActive == true) {
            chatHistory = chatHistory + localCommandGroup("/undo", LocalCommandSource.User, "Cannot undo while the assistant is processing.", LocalCommandResponseKind.Error)
            transcriptVersion++
        } else {
            coroutineScope.launch {
                val response = manager.executeLocalToolCall("undo")
                val message = response.toolCallMessageOrNull() ?: "Undid last change."
                chatHistory = chatHistory + localCommandGroup("/undo", LocalCommandSource.User, message, LocalCommandResponseKind.Help)
                transcriptVersion++
            }
        }
    }
    val performRedo: () -> Unit = {
        if (runningJob?.isActive == true) {
            chatHistory = chatHistory + localCommandGroup("/redo", LocalCommandSource.User, "Cannot redo while the assistant is processing.", LocalCommandResponseKind.Error)
            transcriptVersion++
        } else {
            coroutineScope.launch {
                val response = manager.executeLocalToolCall("redo")
                val message = response.toolCallMessageOrNull() ?: "Redid last change."
                chatHistory = chatHistory + localCommandGroup("/redo", LocalCommandSource.User, message, LocalCommandResponseKind.Help)
                transcriptVersion++
            }
        }
    }

    // Read the snapshot-backed undo/redo availability in the composable body so recomposition
    // observes them, then publish the gated values and actions to the Edit-menu bridge. The
    // assistant gates undo/redo while a turn is running, so the menu mirrors that gating.
    val notRunning = runningJob?.isActive != true
    val canUndoNow = viewModel.canUndoActiveTab && notRunning
    val canRedoNow = viewModel.canRedoActiveTab && notRunning
    SideEffect {
        undoRedoBridge.canUndo = canUndoNow
        undoRedoBridge.canRedo = canRedoNow
        undoRedoBridge.onUndo = performUndo
        undoRedoBridge.onRedo = performRedo
    }

    val showFullChatTranscript = settingsViewModel.getResolvedValue(ShowFullChatTranscriptSetting)
    val effectiveScrollbackPanes = if (showFullChatTranscript) {
        buildDebugScrollbackPanes(chatHistory)
    } else {
        buildSlimScrollbackPanes(chatHistory)
    }
    val systemPrompt = SystemPromptBuilder.build()
    val debugChatHistoryText = buildChatHistoryDebugDumpJson(
        groups = chatHistory,
        providerLabel = providerLabel,
        modelLabel = modelId,
        systemPrompt = systemPrompt,
    )

    return AiChatPaneState.configured(
        providerLabel = providerLabel,
        modelLabel = modelId,
        modelPicker = AiChatPaneModelPickerState(
            selectedModelId = modelId,
            selectedModelLabel = modelOptionsResult?.getOrNull()
                .orEmpty()
                .firstOrNull { it.id == modelId }
                ?.displayName
                ?: modelId,
            options = modelOptionsResult?.getOrNull().orEmpty(),
            isLoading = isModelOptionsLoading,
            errorMessage = modelOptionsResult?.exceptionOrNull()?.message,
            onRefresh = ::loadModelOptions,
            onSelectModel = { selectedModelId ->
                adaptor?.let { settingsViewModel.setUserSetting(it.preferredModelSetting, selectedModelId) }
            },
        ),
        transcriptParts = manager.transcriptParts,
        scrollbackPanes = effectiveScrollbackPanes,
        debugChatHistoryText = debugChatHistoryText,
        isRunning = runningJob?.isActive == true,
        onSubmit = { text ->
            when (val command = parseAiChatLocalCommand(text)) {
                is AiChatLocalCommand.HelpTerse -> {
                    chatHistory = chatHistory + localCommandGroup(command.originalText, LocalCommandSource.User, AiAssistantTerseHelpText.text, LocalCommandResponseKind.Help)
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.HelpFull -> {
                    chatHistory = chatHistory + localCommandGroup(command.originalText, LocalCommandSource.User, AiAssistantFullHelpText.text, LocalCommandResponseKind.Help)
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.Undo -> {
                    performUndo()
                    return@configured
                }
                is AiChatLocalCommand.Redo -> {
                    performRedo()
                    return@configured
                }
                is AiChatLocalCommand.Unknown -> {
                    chatHistory = chatHistory + localCommandGroup(command.originalText, LocalCommandSource.User, "Unknown command: ${command.originalText}. Try /help.", LocalCommandResponseKind.Error)
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
        onProcessingFocusFallback = {
            viewModel.requestFocusAppDropdown()
        },
    )
}


/**
 * Extracts the human-readable message from a tool-call response, if it carries one. Springboard
 * tool handlers return [SpringboardToolCallHandlerResponse]; the dispatcher itself can return a
 * generic [ToolCallExecutionResult] (e.g. unknown tool). Both expose a `message`, but the shared
 * [ToolCallHandlerResponse] marker type does not, so we read it per concrete type.
 */
private fun ToolCallHandlerResponse.toolCallMessageOrNull(): String? = when (this) {
    is SpringboardToolCallHandlerResponse -> message
    is ToolCallExecutionResult -> message
    else -> null
}
