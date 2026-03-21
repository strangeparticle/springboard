package com.strangeparticle.springboard.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.strangeparticle.springboard.app.platform.expandTildePath
import com.strangeparticle.springboard.app.platform.getHomeDirectoryPath
import com.strangeparticle.springboard.app.platform.openFileDialog
import com.strangeparticle.springboard.app.platform.readFileContents
import com.strangeparticle.springboard.app.platform.saveLocalCopyAsFileDialog
import com.strangeparticle.springboard.app.platform.surfaceAppleScriptErrors
import com.strangeparticle.springboard.app.platform.writeFileContents
import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.settings.persistence.DesktopSettingsPersistenceManager
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import com.strangeparticle.springboard.app.ui.dialog.LicenseDialog
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import java.io.File

private enum class ActiveSettingsOpenedFrom {
    SETTINGS_SCREEN,
    MAIN_SCREEN,
}

fun main(args: Array<String>) {
    println("[Springboard] platform initialized")

    val runtimeEnvironment = detectRuntimeEnvironment()

    // Initialize the settings system
    val persistenceManager = DesktopSettingsPersistenceManager()
    val settingsManager = SettingsManager(runtimeEnvironment, persistenceManager)

    // Load all settings sources: persisted user settings, env vars, CLI args
    settingsManager.loadSettingsAtStartup(
        environmentVariables = System.getenv(),
        commandLineArgs = args.toList(),
    )

    // Propagate the resolved surfaceAppleScriptErrors value to the browser automation module
    surfaceAppleScriptErrors = settingsManager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)

    // Determine the startup config path from the explicit startup-springboard setting.
    val startupSpringboardPath = settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)?.path
    val configPath = startupSpringboardPath

    println("[Springboard] launch config path: ${configPath ?: "none"}")
    if (settingsManager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)) {
        println("[Springboard] AppleScript error surfacing enabled")
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(700.dp, 250.dp),
            position = WindowPosition(Alignment.Center)
        )

        val viewModel = remember { SpringboardViewModel(settingsManager) }
        val settingsViewModel = remember {
            SettingsViewModel(
                settingsManager = settingsManager,
                currentFilePath = { viewModel.springboard?.source },
            )
        }
        val firstDropdownFocusRequester = remember { FocusRequester() }
        val showSettings = remember { mutableStateOf(false) }
        val showActiveSettings = remember { mutableStateOf(false) }
        val showLicenseDialog = remember { mutableStateOf(false) }
        val activeSettingsOpenedFrom = remember { mutableStateOf<ActiveSettingsOpenedFrom?>(null) }
        val loadSpringboardConfig: (String, String) -> Unit = { path, contents ->
            println("[Springboard] config loading: $path")
            viewModel.loadConfig(contents, path)
            println("[Springboard] grid ready")
        }
        val openSettingsScreen = {
            activeSettingsOpenedFrom.value = null
            showActiveSettings.value = false
            showSettings.value = true
        }
        val openActiveSettingsFromSettings = {
            activeSettingsOpenedFrom.value = ActiveSettingsOpenedFrom.SETTINGS_SCREEN
            showActiveSettings.value = true
        }
        val openActiveSettingsFromMain = {
            activeSettingsOpenedFrom.value = ActiveSettingsOpenedFrom.MAIN_SCREEN
            showSettings.value = true
            showActiveSettings.value = true
        }
        val closeActiveSettings = {
            when (activeSettingsOpenedFrom.value) {
                ActiveSettingsOpenedFrom.SETTINGS_SCREEN -> {
                    showActiveSettings.value = false
                }
                ActiveSettingsOpenedFrom.MAIN_SCREEN -> {
                    showActiveSettings.value = false
                    showSettings.value = false
                }
                null -> {
                    showActiveSettings.value = false
                }
            }
            activeSettingsOpenedFrom.value = null
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Springboard",
            state = windowState
        ) {
            SpringboardMenuBar(
                onOpen = {
                    val path = openFileDialog(null)
                    if (path != null) {
                        val contents = readFileContents(path)
                        if (contents != null) {
                            loadSpringboardConfig(path, contents)
                        } else {
                            ToastBroadcaster.error("Failed to open: file not found")
                        }
                    }
                },
                onSaveLocalCopyAs = {
                    val springboard = viewModel.springboard
                    if (springboard == null) {
                        ToastBroadcaster.warning("No springboard is loaded")
                        return@SpringboardMenuBar
                    }
                    val suggestedName = springboard.name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "") + ".json"
                    val path = saveLocalCopyAsFileDialog(suggestedName)
                    if (path != null) {
                        val success = writeFileContents(path, springboard.jsonSource)
                        if (success) {
                            ToastBroadcaster.info("Saved to $path")
                        } else {
                            ToastBroadcaster.error("Failed to save to $path")
                        }
                    }
                },
                onReload = {
                    val springboard = viewModel.springboard ?: return@SpringboardMenuBar
                    val path = springboard.source
                    val contents = readFileContents(path)
                    if (contents != null) {
                        loadSpringboardConfig(path, contents)
                    } else {
                        ToastBroadcaster.error("Failed to reload: file not found")
                    }
                },
                onOpenSettings = openSettingsScreen,
                onShowActiveSettings = openActiveSettingsFromMain,
                onShowLicense = {
                    showLicenseDialog.value = true
                },
            )

            if (showLicenseDialog.value) {
                LicenseDialog(
                    onClose = {
                        showLicenseDialog.value = false
                    }
                )
            }

            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                firstDropdownFocusRequester = firstDropdownFocusRequester,
                showSettings = showSettings,
                showActiveSettings = showActiveSettings,
                onOpenSettings = openSettingsScreen,
                onOpenActiveSettingsFromSettings = openActiveSettingsFromSettings,
                onCloseActiveSettings = closeActiveSettings,
                onRequestFocusFirstDropdown = {
                    try {
                        firstDropdownFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            )

            // Runs once initially and again whenever the loaded springboard instance changes.
            LaunchedEffect(viewModel.springboard) {
                resizeWindowToFitSpringboard(viewModel, windowState)
            }

            // Runs once when this window composition starts because configPath is captured from main(args).
            // It handles optional startup loading from the launch argument, then marks startup complete.
            LaunchedEffect(configPath) {
                if (configPath != null) {
                    val homeDirectoryPath = getHomeDirectoryPath()
                    val expandedConfigPath = expandTildePath(configPath, homeDirectoryPath)
                    val file = File(expandedConfigPath)
                    if (file.exists()) {
                        val contents = file.readText()
                        loadSpringboardConfig(expandedConfigPath, contents)
                    } else {
                        ToastBroadcaster.error("Config file not found: $expandedConfigPath")
                        println("[Springboard] config file not found: $expandedConfigPath")
                    }
                }

                println("[Springboard] application ready")
            }

            val windowInfo = LocalWindowInfo.current
            // Runs when the window focus state changes; when focus returns, restore keyboard focus.
            LaunchedEffect(windowInfo.isWindowFocused) {
                if (windowInfo.isWindowFocused && viewModel.isConfigLoaded) {
                    // Small delay to ensure composition is ready before requesting focus
                    kotlinx.coroutines.delay(50)
                    try {
                        firstDropdownFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
