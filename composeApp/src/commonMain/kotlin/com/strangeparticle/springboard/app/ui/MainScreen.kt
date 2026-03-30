package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.strangeparticle.springboard.app.domain.factory.currentTimeMillis
import com.strangeparticle.springboard.app.platform.readFileContents
import com.strangeparticle.springboard.app.ui.gridnav.GridNav
import com.strangeparticle.springboard.app.ui.keynav.NavBar
import com.strangeparticle.springboard.app.ui.openbutton.OpenSpringboardPrompt
import com.strangeparticle.springboard.app.ui.activatorpreview.ActivatorPreview
import com.strangeparticle.springboard.app.ui.statusbar.StatusBar
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: SpringboardViewModel,
    firstDropdownFocusRequester: FocusRequester,
    isShiftHeld: Boolean,
    onOpenSettings: () -> Unit,
) {
    var lastLoadedPath by remember { mutableStateOf<String?>(null) }
    var isReloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(
            viewModel = viewModel,
            firstDropdownFocusRequester = firstDropdownFocusRequester
        )

        LaunchedEffect(viewModel.isConfigLoaded) {
            if (viewModel.isConfigLoaded) {
                // Delay lets the dropdown composables enter the tree before focus is requested
                kotlinx.coroutines.delay(100)
                try { firstDropdownFocusRequester.requestFocus() } catch (_: Exception) {}
            }
        }

        if (!viewModel.isConfigLoaded) {
            OpenSpringboardPrompt(
                onFileSelected = { path ->
                    val contents = readFileContents(path)
                    if (contents != null) {
                        lastLoadedPath = path
                        viewModel.loadConfig(contents, path)
                        println("[Springboard] grid ready")
                        println("[Springboard] application ready")
                    }
                }
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                GridNav(
                    viewModel = viewModel,
                    isShiftHeld = isShiftHeld,
                    onShiftRelease = {
                        if (viewModel.multiSelectSet.isNotEmpty()) {
                            viewModel.activateMultiSelect()
                        }
                    }
                )
            }

            ActivatorPreview(previewText = viewModel.hoveredActivatorPreview)

            StatusBar(
                springboard = viewModel.springboard,
                isReloading = isReloading,
                onReload = {
                    val path = lastLoadedPath ?: viewModel.springboard?.source ?: return@StatusBar
                    scope.launch {
                        isReloading = true
                        val startTime = currentTimeMillis()
                        try {
                            val contents = readFileContents(path)
                            if (contents != null) {
                                viewModel.loadConfig(contents, path)
                            } else {
                                ToastBroadcaster.error("Failed to reload: file not found")
                            }
                        } catch (e: Exception) {
                            ToastBroadcaster.error("Failed to reload: ${e.message}")
                        }
                        val elapsed = currentTimeMillis() - startTime
                        if (elapsed < CommonUiConstants.ReloadSpinMinMs) {
                            delay(CommonUiConstants.ReloadSpinMinMs - elapsed)
                        }
                        isReloading = false
                    }
                },
                onOpenSettings = onOpenSettings,
            )
        }
    }
}
