package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import com.strangeparticle.editio.client.provider.AiProvider
import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.editio.session.AiSessionManager
import com.strangeparticle.editio.session.AiSessionSnapshotProvider
import com.strangeparticle.editio.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.editio.session.event.LocalCommandRespondedAiChatEvent
import com.strangeparticle.editio.session.event.LocalCommandResponseKind
import com.strangeparticle.editio.session.event.LocalCommandSource
import com.strangeparticle.editio.session.event.LocalCommandSubmittedAiChatEvent
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
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.editio.AiChatLocalCommand
import com.strangeparticle.springboard.app.ui.editio.AiChatPaneState
import com.strangeparticle.springboard.app.ui.editio.buildDebugScrollbackPanes
import com.strangeparticle.springboard.app.ui.editio.buildSlimScrollbackPanes
import com.strangeparticle.springboard.app.ui.editio.initialTerseHelpEvents
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

    val aiClient = remember(provider, isConfigured, modelId) {
        if (provider != null && isConfigured) provider.createClient(context) else null
    }
    var transcriptVersion by remember { mutableStateOf(0) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    // TODO: what's this change doing here?  Seems like this should be part of a different commit?
    val latestModelId by rememberUpdatedState(modelId)
    var chatEvents by remember(aiClient, viewModel) {
        mutableStateOf(initialTerseHelpEvents())
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
            eventsProvider = { chatEvents },
            appendEvents = { events -> chatEvents = chatEvents + events },
            onTranscriptChanged = { transcriptVersion++ },
        )
    }
    transcriptVersion
    val showFullChatTranscript = settingsViewModel.getResolvedValue(ShowFullChatTranscriptSetting)
    val effectiveScrollbackPanes = if (showFullChatTranscript) {
        buildDebugScrollbackPanes(chatEvents)
    } else {
        buildSlimScrollbackPanes(chatEvents)
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
                    chatEvents = chatEvents + listOf(
                        LocalCommandSubmittedAiChatEvent(command.originalText, LocalCommandSource.User),
                        LocalCommandRespondedAiChatEvent(command.originalText, AiAssistantTerseHelpText.text, LocalCommandResponseKind.Help),
                    )
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.HelpFull -> {
                    chatEvents = chatEvents + listOf(
                        LocalCommandSubmittedAiChatEvent(command.originalText, LocalCommandSource.User),
                        LocalCommandRespondedAiChatEvent(command.originalText, AiAssistantFullHelpText.text, LocalCommandResponseKind.Help),
                    )
                    transcriptVersion++
                    return@configured
                }
                is AiChatLocalCommand.Unknown -> {
                    chatEvents = chatEvents + listOf(
                        LocalCommandSubmittedAiChatEvent(command.originalText, LocalCommandSource.User),
                        LocalCommandRespondedAiChatEvent(
                            command.originalText,
                            "Unknown command: ${command.originalText}. Try /help.",
                            LocalCommandResponseKind.Error,
                        ),
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
        onProcessingFocusFallback = {
            viewModel.requestFocusAppDropdown()
        },
    )
}


private fun createSpringboardToolCallRegistry(): ToolCallRegistry = ToolCallRegistry().apply {
    register(ActivateColumnToolCallHandler())
    register(ActivateCoordinateToolCallHandler())
    register(ActivateCoordinatesToolCallHandler())
    register(ActivateRowToolCallHandler())
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
