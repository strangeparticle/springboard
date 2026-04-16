package com.strangeparticle.springboard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.settings.ActiveSettingsScreen
import com.strangeparticle.springboard.app.ui.settings.SettingsScreen
import com.strangeparticle.springboard.app.ui.toast.ToastOverlay
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

@Composable
fun SpringboardApp(
    viewModel: SpringboardViewModel,
    settingsViewModel: SettingsViewModel,
    firstDropdownFocusRequester: FocusRequester,
    showSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
    showActiveSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
    activeSettingsOpenedFromSettings: MutableState<Boolean> = remember { mutableStateOf(false) },
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
    onRequestFocusFirstDropdown: (() -> Unit)? = null,
    fileContentService: PlatformFileContentService = PlatformFileContentServiceDefaultImpl(),
    networkContentService: NetworkContentService? = null,
    showFileOpen: Boolean = true,
) {
    var isShiftHeld by remember { mutableStateOf(false) }

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
                        onBack = { showSettings.value = false },
                        onShowActiveSettings = onOpenActiveSettingsFromSettings,
                    )
                }
            } else {
                MainScreen(
                    viewModel = viewModel,
                    firstDropdownFocusRequester = firstDropdownFocusRequester,
                    isShiftHeld = isShiftHeld,
                    onOpenSettings = onOpenSettings,
                    fileContentService = fileContentService,
                    networkContentService = networkContentService,
                    showFileOpen = showFileOpen,
                )
            }

            ToastOverlay(
                tabToastState = viewModel.activeTabToast,
                isTabVisible = !showSettings.value,
                onToastDismissed = {
                    onRequestFocusFirstDropdown?.invoke()
                },
            )
        }
    }
}
