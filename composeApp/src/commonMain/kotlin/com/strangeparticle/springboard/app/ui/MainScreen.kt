package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.factory.currentTimeMillis
import com.strangeparticle.springboard.app.domain.parseSpringboardSource
import com.strangeparticle.springboard.app.domain.toHttpsUrl
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.ui.gridnav.GridNav
import com.strangeparticle.springboard.app.ui.keynav.NavBar
import com.strangeparticle.springboard.app.ui.openbutton.OpenFromNetworkDialog
import com.strangeparticle.springboard.app.ui.openbutton.WelcomeScreen
import com.strangeparticle.springboard.app.ui.activatorpreview.ActivatorPreview
import com.strangeparticle.springboard.app.ui.statusbar.StatusBar
import com.strangeparticle.springboard.app.ui.tabs.TabBar
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: SpringboardViewModel,
    isShiftHeld: Boolean,
    onOpenSettings: () -> Unit,
    fileContentService: PlatformFileContentService = PlatformFileContentServiceDefaultImpl(),
    networkContentService: NetworkContentService? = null,
    showFileOpen: Boolean = true,
) {
    var lastLoadedPath by remember { mutableStateOf<String?>(null) }
    var isReloading by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }
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
                    lastLoadedPath = url
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
        NavBar(viewModel = viewModel)

        if (activeTab == null || activeTab.isEmpty) {
            Box(modifier = Modifier.weight(1f)) {
                WelcomeScreen(
                    onFileSelected = { path ->
                        val contents = fileContentService.readFileContents(path)
                        if (contents != null) {
                            lastLoadedPath = path
                            viewModel.loadConfig(contents, path)
                            println("[Springboard] grid ready")
                            println("[Springboard] application ready")
                        }
                    },
                    onOpenFromNetwork = openFromNetwork,
                    showFileOpen = showFileOpen,
                )
            }
        } else {
            val currentSpringboard = activeTab.springboard
            val currentEnvironmentId = activeTab.selectedEnvironmentId

            Box(modifier = Modifier.weight(1f)) {
                if (currentSpringboard != null && currentEnvironmentId != null) {
                    GridNav(
                        springboard = currentSpringboard,
                        selectedEnvironmentId = currentEnvironmentId,
                        multiSelectSet = viewModel.multiSelectSet,
                        keyNavCoordinate = viewModel.keyNavCoordinate,
                        isShiftHeld = isShiftHeld,
                        onCellActivate = { viewModel.activateCell(it) },
                        onColumnActivate = { viewModel.activateColumn(it) },
                        onRowActivate = { viewModel.activateRow(it) },
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
                    val source = lastLoadedPath ?: viewModel.springboard?.source ?: return@StatusBar
                    scope.launch {
                        isReloading = true
                        val startTime = currentTimeMillis()
                        try {
                            when (val parsed = parseSpringboardSource(source)) {
                                is SpringboardSource.HttpSource -> {
                                    if (networkContentService != null) {
                                        val contents = networkContentService.fetchText(parsed.url)
                                        viewModel.loadConfig(contents, source)
                                    } else {
                                        viewModel.activeTabToast.error("Network reload not available")
                                    }
                                }
                                is SpringboardSource.S3Source -> {
                                    if (networkContentService != null) {
                                        val contents = networkContentService.fetchText(parsed.toHttpsUrl())
                                        viewModel.loadConfig(contents, source)
                                    } else {
                                        viewModel.activeTabToast.error("Network reload not available")
                                    }
                                }
                                is SpringboardSource.FileSource -> {
                                    val contents = fileContentService.readFileContents(parsed.path)
                                    if (contents != null) {
                                        viewModel.loadConfig(contents, source)
                                    } else {
                                        viewModel.activeTabToast.error("Failed to reload: file not found")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            viewModel.activeTabToast.error("Failed to reload: ${e.message}")
                        }
                        val elapsed = currentTimeMillis() - startTime
                        if (elapsed < CommonUiConstants.ReloadSpinMinMs) {
                            delay(CommonUiConstants.ReloadSpinMinMs - elapsed)
                        }
                        isReloading = false
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
            onClose = { viewModel.closeTab(it) },
            onCreate = { viewModel.createTab() },
            onOpenSettings = onOpenSettings,
        )
    }
}
