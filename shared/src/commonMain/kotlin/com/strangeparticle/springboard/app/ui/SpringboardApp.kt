package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalWindowInfo
import com.strangeparticle.luther.core.client.provider.AiProvider
import com.strangeparticle.luther.core.client.provider.LutherBuiltInProviders
import com.strangeparticle.luther.core.client.provider.LutherProviderCatalog
import com.strangeparticle.luther.core.session.AiSessionSnapshotProvider
import com.strangeparticle.luther.core.session.AiSessionToolCallExecutionContextFactory
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
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.settings.items.core.AiProviderSetting
import com.strangeparticle.springboard.app.settings.items.core.ShowFullChatTranscriptSetting
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.luther.cmp.AiChatPaneState
import com.strangeparticle.luther.cmp.rememberAiAssistant
import com.strangeparticle.luther.core.session.LutherSettings
import com.strangeparticle.springboard.app.ui.settings.ActiveSettingsScreen
import com.strangeparticle.springboard.app.ui.settings.SettingsScreen
import com.strangeparticle.springboard.app.ui.toast.ToastOverlay
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

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
    val adaptor = AiProviderSettingsAdaptorRegistry.byId(selectedProviderId)
    val providerConfig = adaptor?.buildProviderConfig(settingsViewModel)
    val modelId = adaptor?.let { settingsViewModel.getResolvedValue(it.preferredModelSetting) }.orEmpty()
    val httpClient = settingsViewModel.aiHttpClient
    val builtInProviders = remember(httpClient) { LutherBuiltInProviders.all(httpClient) }
    val provider: AiProvider? = builtInProviders.firstOrNull { it.id == selectedProviderId }
    val isConfigured = provider != null && providerConfig != null && provider.isConfigured(providerConfig)
    val catalog = remember(builtInProviders) { LutherProviderCatalog(builtInProviders) }

    val settings: LutherSettings? =
        if (provider != null && providerConfig != null && isConfigured && modelId.isNotBlank()) {
            LutherSettings(selectedProviderId, modelId, providerConfig)
        } else {
            null
        }

    val snapshotProvider = remember(viewModel) {
        object : AiSessionSnapshotProvider {
            override fun getSnapshotJson(): String = SpringboardAppSnapshot.capture(viewModel).toCompactJson()
        }
    }
    val executionContextFactory = remember(viewModel) {
        object : AiSessionToolCallExecutionContextFactory {
            override fun createToolCallExecutionContext(
                onStateChanged: () -> Unit,
                awaitUserApproval: suspend (toolCallId: String) -> Boolean,
            ): ToolCallExecutionContext = object : SpringboardToolCallExecutionContext {
                override val viewModel: SpringboardViewModel = viewModel
                override fun markStateChanged() = onStateChanged()
                override suspend fun awaitUserApproval(toolCallId: String): Boolean = awaitUserApproval(toolCallId)
            }
        }
    }
    val toolHandlers = remember { springboardToolCallHandlers() }
    val showFullChatTranscript = settingsViewModel.getResolvedValue(ShowFullChatTranscriptSetting)

    val assistant = rememberAiAssistant(
        providers = builtInProviders,
        settings = settings,
        catalog = catalog,
        toolHandlers = toolHandlers,
        snapshotProvider = snapshotProvider,
        executionContextFactory = executionContextFactory,
        systemPromptProvider = { SystemPromptBuilder.build() },
        terseHelpText = AiAssistantTerseHelpText.text,
        fullHelpText = AiAssistantFullHelpText.text,
        showFullTranscript = showFullChatTranscript,
        onTurnStart = { viewModel.beginEditTransaction() },
        onTurnEnd = { viewModel.commitEditTransaction() },
        onModelSelected = { selectedModelId ->
            adaptor?.let { settingsViewModel.setUserSetting(it.preferredModelSetting, selectedModelId) }
        },
        onProcessingFocusFallback = { viewModel.requestFocusAppDropdown() },
        toolMessageExtractor = { it.toolCallMessageOrNull() },
    )

    // The assistant gates undo/redo while a turn is running; the Edit menu mirrors that gating and
    // dispatches through the assistant so the action is also recorded in the chat history.
    val notRunning = !assistant.isRunning
    val canUndoNow = settings != null && viewModel.canUndoActiveTab && notRunning
    val canRedoNow = settings != null && viewModel.canRedoActiveTab && notRunning
    SideEffect {
        undoRedoBridge.canUndo = canUndoNow
        undoRedoBridge.canRedo = canRedoNow
        undoRedoBridge.onUndo = { assistant.performUndo() }
        undoRedoBridge.onRedo = { assistant.performRedo() }
    }

    return assistant.paneState
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
