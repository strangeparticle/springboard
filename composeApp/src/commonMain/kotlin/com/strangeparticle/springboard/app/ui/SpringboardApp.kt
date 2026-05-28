package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.client.provider.AiProviderRegistry
import com.strangeparticle.luther.session.AiSessionManager
import com.strangeparticle.luther.session.AiSessionSnapshotProvider
import com.strangeparticle.luther.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.luther.session.ChatHistoryGroup
import com.strangeparticle.luther.session.ChatHistoryGroupType
import com.strangeparticle.luther.session.buildChatHistoryDebugDumpJson
import com.strangeparticle.luther.session.event.LocalCommandRespondedChatHistoryItem
import com.strangeparticle.luther.session.event.LocalCommandResponseKind
import com.strangeparticle.luther.session.event.LocalCommandSource
import com.strangeparticle.luther.session.event.LocalCommandSubmittedChatHistoryItem
import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.luther.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.luther.SpringboardToolCallExecutionContext
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
import com.strangeparticle.springboard.app.ui.luther.initialTerseHelpHistory
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
) {
    var isShiftHeld by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var aiSettingsFirst by remember { mutableStateOf(false) }
    val derivedAiChatPaneState = rememberAiChatPaneState(
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
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
                            viewModel.requestFocusAppDropdown()
                        },
                        onShowActiveSettings = onOpenActiveSettingsFromSettings,
                        currentTabSources = viewModel.currentTabSources,
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
                    openFileDialog = openFileDialog,
                    isAssistantConfigured = effectiveAiChatPaneState.isConfigured,
                    onToggleAssistant = { showAssistant.value = !showAssistant.value },
                    showAssistant = showAssistant.value,
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
): AiChatPaneState {
    settingsViewModel.settingsVersion
    val selectedProviderId = settingsViewModel.getResolvedValue(AiProviderSetting)
    val provider: AiProvider? = AiProviderRegistry.byId(selectedProviderId)
    val context = settingsViewModel.itemContext()
    val isConfigured = provider != null && provider.isConfigured(context)
    val modelId = provider?.currentModelId(context).orEmpty()
    val modelSetting = provider?.preferredModelSetting()
    var modelOptionsResult by remember(modelSetting) { mutableStateOf<Result<List<DropDownOption>>?>(null) }
    var isModelOptionsLoading by remember(modelSetting) { mutableStateOf(false) }

    fun loadModelOptions() {
        val activeModelSetting = modelSetting ?: return
        coroutineScope.launch {
            isModelOptionsLoading = true
            modelOptionsResult = activeModelSetting.loadOptions(settingsViewModel.itemContext())
            isModelOptionsLoading = false
        }
    }
    LaunchedEffect(modelSetting, isConfigured, settingsViewModel.settingsVersion) {
        if (modelSetting != null && isConfigured) loadModelOptions()
    }

    val aiClient = remember(provider, isConfigured, modelId) {
        if (provider != null && isConfigured) provider.createClient(context) else null
    }
    var transcriptVersion by remember { mutableStateOf(0) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    // TODO: what's this change doing here?  Seems like this should be part of a different commit?
    val latestModelId by rememberUpdatedState(modelId)
    var chatHistory by remember(aiClient, viewModel) {
        mutableStateOf(initialTerseHelpHistory())
    }

    if (provider == null || aiClient == null || modelId.isBlank()) {
        return AiChatPaneState.notConfigured()
    }

    val manager = remember(aiClient, viewModel) {
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
            modelIdProvider = { latestModelId },
            coroutineScope = coroutineScope,
            groupsProvider = { chatHistory },
            updateGroups = { groups -> chatHistory = groups },
            onTranscriptChanged = { transcriptVersion++ },
        )
    }
    transcriptVersion
    val showFullChatTranscript = settingsViewModel.getResolvedValue(ShowFullChatTranscriptSetting)
    val effectiveScrollbackPanes = if (showFullChatTranscript) {
        buildDebugScrollbackPanes(chatHistory)
    } else {
        buildSlimScrollbackPanes(chatHistory)
    }
    val systemPrompt = SystemPromptBuilder.build()
    val debugChatHistoryText = buildChatHistoryDebugDumpJson(
        groups = chatHistory,
        providerLabel = provider.displayName,
        modelLabel = modelId,
        systemPrompt = systemPrompt,
    )

    return AiChatPaneState.configured(
        providerLabel = provider.displayName,
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
                modelSetting?.let { settingsViewModel.setUserSetting(it, selectedModelId) }
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
                    if (runningJob?.isActive == true) {
                        chatHistory = chatHistory + localCommandGroup(command.originalText, LocalCommandSource.User, "Cannot undo while the assistant is processing.", LocalCommandResponseKind.Error)
                        transcriptVersion++
                        return@configured
                    }
                    val lastAiIndex = chatHistory.indexOfLast { it.type == ChatHistoryGroupType.AI_INTERACTION }
                    if (lastAiIndex < 0) {
                        chatHistory = chatHistory + localCommandGroup(command.originalText, LocalCommandSource.User, "Nothing to undo.", LocalCommandResponseKind.Error)
                        transcriptVersion++
                        return@configured
                    }
                    val undoGroup = chatHistory[lastAiIndex]
                    val snapshotJson = undoGroup.preSnapshotJson
                    if (snapshotJson != null) {
                        try {
                            val snapshot = SpringboardAppSnapshot.fromJson(snapshotJson)
                            val tabSnapshot = snapshot.tabs.firstOrNull { it.tabId == viewModel.activeTabId }
                            if (tabSnapshot?.springboard != null) {
                                val springboard = SpringboardFactory.fromDto(tabSnapshot.springboard, tabSnapshot.source ?: "")
                                viewModel.suppressWindowGrow = true
                                viewModel.restoreTabFromUndoSnapshot(
                                    tabId = tabSnapshot.tabId,
                                    springboard = springboard,
                                    label = tabSnapshot.label,
                                    isDirty = tabSnapshot.isDirty,
                                )
                                viewModel.suppressWindowGrow = false
                            }
                        } catch (_: Exception) { }
                    }
                    chatHistory = chatHistory.filterIndexed { index, _ -> index != lastAiIndex }
                    manager.markExternalStateChange()
                    transcriptVersion++
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


private fun localCommandGroup(
    commandText: String,
    source: LocalCommandSource,
    responseText: String,
    responseKind: LocalCommandResponseKind,
): ChatHistoryGroup = ChatHistoryGroup(
    type = ChatHistoryGroupType.LOCAL_COMMAND,
    items = listOf(
        LocalCommandSubmittedChatHistoryItem(commandText, source),
        LocalCommandRespondedChatHistoryItem(commandText, responseText, responseKind),
    ),
)
