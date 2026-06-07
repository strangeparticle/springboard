package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.domain.factory.currentTimeMillis
import com.strangeparticle.springboard.app.domain.model.hasAnyAllEnvsActivators
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.ui.gridnav.GridNav
import com.strangeparticle.springboard.app.ui.luther.AiChatPane
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneDefaults
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneState
import com.strangeparticle.springboard.app.ui.luther.ChatPaneResizeHandle
import com.strangeparticle.springboard.app.ui.keynav.NavBar
import com.strangeparticle.springboard.app.ui.openbutton.OpenFromNetworkDialog
import com.strangeparticle.springboard.app.ui.openbutton.WelcomeScreen
import com.strangeparticle.springboard.app.ui.activatorpreview.ActivatorPreview
import com.strangeparticle.springboard.app.ui.statusbar.StatusBar
import com.strangeparticle.springboard.app.ui.tabs.CloseDirtyTabConfirmDialog
import com.strangeparticle.springboard.app.ui.tabs.TabBar
import com.strangeparticle.springboard.app.ui.tabs.requestTabClose
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun MainScreen(
    viewModel: SpringboardViewModel,
    isShiftHeld: Boolean,
    onOpenSettings: () -> Unit,
    fileContentService: PlatformFileContentService = PlatformFileContentServiceDefaultImpl(),
    networkContentService: NetworkContentService? = null,
    showFileOpen: Boolean = true,
    isAssistantConfigured: Boolean = false,
    onToggleAssistant: () -> Unit = {},
    showAssistant: Boolean = false,
    aiChatPaneState: AiChatPaneState = AiChatPaneState.notConfigured(),
    onCloseAssistant: () -> Unit = {},
    onOpenAiSettings: () -> Unit = onOpenSettings,
    openFileDialog: () -> String? = { com.strangeparticle.springboard.app.platform.openFileDialog(null) },
) {
    var isReloading by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var pendingCloseTabId by remember { mutableStateOf<String?>(null) }
    val assistantInputFocusRequester = remember { FocusRequester() }
    val environmentDropdownFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val openFromNetwork: (() -> Unit)? = if (networkContentService != null) {
        { showNetworkDialog = true }
    } else {
        null
    }

    val loadFromNetwork: (String) -> Unit = { url ->
        if (networkContentService != null) {
            scope.launch {
                try {
                    val contents = networkContentService.fetchText(url)
                    viewModel.loadConfig(contents, url)
                    println("[Springboard] grid ready")
                    println("[Springboard] application ready")
                } catch (e: Exception) {
                    viewModel.activeTabToast.error("Failed to fetch: ${e.message}")
                }
            }
        }
    }

    if (showNetworkDialog) {
        OpenFromNetworkDialog(
            onConfirm = { url ->
                showNetworkDialog = false
                loadFromNetwork(url)
            },
            onDismiss = { showNetworkDialog = false },
        )
    }

    val activeTab = viewModel.activeTab

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(
            viewModel = viewModel,
            onTabOutForward = if (showAssistant) {
                { try { assistantInputFocusRequester.requestFocus() } catch (_: Exception) {} }
            } else {
                null
            },
            onTabOutBackward = if (showAssistant) {
                { try { assistantInputFocusRequester.requestFocus() } catch (_: Exception) {} }
            } else {
                null
            },
            environmentDropdownFocusRequester = environmentDropdownFocusRequester,
        )

        if (activeTab == null || activeTab.isEmpty) {
            Box(modifier = Modifier.weight(1f)) {
                WelcomeScreen(
                    onFileSelected = { path ->
                        val contents = fileContentService.readFileContents(path)
                        if (contents != null) {
                            viewModel.loadConfig(contents, path)
                            viewModel.requestFocusAppDropdown()
                            println("[Springboard] grid ready")
                            println("[Springboard] application ready")
                        }
                    },
                    onOpenFromNetwork = openFromNetwork,
                    showFileOpen = showFileOpen,
                    openFileDialog = openFileDialog,
                )
            }
        } else {
            val currentSpringboardFilteredForRuntime = activeTab.springboardFilteredForRuntime
            val currentEnvironmentId = activeTab.selectedEnvironmentId

            Box(modifier = Modifier.weight(1f)) {
                if (currentSpringboardFilteredForRuntime != null &&
                    (currentEnvironmentId != null || currentSpringboardFilteredForRuntime.hasAnyAllEnvsActivators())
                ) {
                    GridNav(
                        springboard = currentSpringboardFilteredForRuntime,
                        selectedEnvironmentId = currentEnvironmentId,
                        environments = currentSpringboardFilteredForRuntime.environments,
                        multiSelectSet = viewModel.multiSelectSet,
                        keyNavCoordinate = viewModel.keyNavCoordinate,
                        isShiftHeld = isShiftHeld,
                        onCellActivate = { viewModel.activateCell(it) },
                        onColumnActivate = { environmentId, appId -> viewModel.activateColumn(environmentId, appId) },
                        onRowActivate = { environmentId, resourceId -> viewModel.activateRow(environmentId, resourceId) },
                        onEnvironmentHeadingSelect = { viewModel.selectEnvironmentFromGridHeading(it) },
                        onToggleMultiSelect = { viewModel.toggleMultiSelect(it) },
                        onActivatorPreviewChange = { viewModel.hoveredActivatorPreview = it },
                        zoomSelection = viewModel.gridZoomSelection,
                    )
                }
            }

            ActivatorPreview(previewText = viewModel.hoveredActivatorPreview)

            StatusBar(
                activeTab = activeTab,
                isReloading = isReloading,
                onZoomSelectionChange = { viewModel.gridZoomSelection = it },
                onReload = {
                    if (viewModel.springboardFilteredForRuntime?.source == null) return@StatusBar
                    scope.launch {
                        isReloading = true
                        try {
                            val startTime = currentTimeMillis()
                            viewModel.reloadCurrentSource()
                            val elapsed = currentTimeMillis() - startTime
                            if (elapsed < CommonUiConstants.ReloadSpinMinMs) {
                                delay(CommonUiConstants.ReloadSpinMinMs - elapsed)
                            }
                        } finally {
                            isReloading = false
                        }
                    }
                },
                onOpenFromNetwork = openFromNetwork,
            )
        }

        TabBar(
            tabs = viewModel.tabs,
            activeTabId = viewModel.activeTabId,
            canCreateNewTab = viewModel.canCreateNewTab,
            onSelect = { viewModel.selectTab(it) },
            onClose = { tabId ->
                requestTabClose(tabId, viewModel) { pendingCloseTabId = it }
            },
            onCreate = { viewModel.createTab() },
        )

        if (showAssistant) {
            var chatPaneHeightDp by rememberSaveable {
                mutableStateOf(AiChatPaneDefaults.DefaultHeight.value)
            }
            val density = LocalDensity.current
            ChatPaneResizeHandle(
                onDragDelta = { deltaPx ->
                    // Drag DOWN (positive deltaPx) shrinks the pane (pane grows upward from
                    // its bottom edge, which is pinned to the BottomBar below).
                    val deltaDp = with(density) { deltaPx.toDp() }
                    val proposed = (chatPaneHeightDp.dp - deltaDp).coerceIn(
                        AiChatPaneDefaults.MinHeight,
                        AiChatPaneDefaults.MaxHeight,
                    )
                    chatPaneHeightDp = proposed.value
                },
            )
            AiChatPane(
                state = aiChatPaneState,
                onClose = onCloseAssistant,
                onOpenSettings = onOpenAiSettings,
                onTabOut = { viewModel.requestFocusAppDropdown() },
                onShiftTabOut = { try { environmentDropdownFocusRequester.requestFocus() } catch (_: Exception) {} },
                inputFocusRequester = assistantInputFocusRequester,
                height = chatPaneHeightDp.dp,
            )
        }

        AppBottomBar(
            isAssistantConfigured = isAssistantConfigured,
            isAssistantOpen = showAssistant,
            onToggleAssistant = onToggleAssistant,
            onOpenSettings = onOpenSettings,
        )
    }

    val closingTabId = pendingCloseTabId
    if (closingTabId != null) {
        val tab = viewModel.tabs.firstOrNull { it.tabId == closingTabId }
        if (tab == null) {
            // Tab vanished while the dialog was queued — clear the pending state.
            pendingCloseTabId = null
        } else {
            CloseDirtyTabConfirmDialog(
                tabLabel = tab.label,
                onCancel = { pendingCloseTabId = null },
                onCloseAnyway = {
                    viewModel.closeTab(closingTabId)
                    pendingCloseTabId = null
                },
            )
        }
    }
}
