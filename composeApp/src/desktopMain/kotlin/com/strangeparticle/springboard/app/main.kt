package com.strangeparticle.springboard.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.strangeparticle.editio.client.AiClient
import com.strangeparticle.editio.client.provider.anthropic.AiClientAnthropic
import com.strangeparticle.editio.client.provider.openai.AiClientOpenAi
import com.strangeparticle.springboard.app.persistence.PersistenceServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.*
import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import com.strangeparticle.springboard.app.ui.dialog.LicenseDialog
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoaderDesktopImpl
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.viewmodel.TabRestorer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.launch
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.io.File
private enum class ActiveSettingsOpenedFrom {
    SETTINGS_SCREEN,
    MAIN_SCREEN,
}

fun main(args: Array<String>) {
    println("[Springboard] platform initialized")

    val runtimeEnvironment = detectRuntimeEnvironment()

    // Initialize services
    val persistenceService = PersistenceServiceDefaultImpl()
    val settingsManager = SettingsManager(runtimeEnvironment, persistenceService)

    // Load all settings sources: persisted user settings, env vars, CLI args
    settingsManager.loadSettingsAtStartup(
        environmentVariables = System.getenv(),
        commandLineArgs = args.toList(),
    )

    // Propagate the resolved surfaceAppleScriptErrors value to the browser automation module
    val resolvedSurfaceAppleScriptErrors = settingsManager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
    surfaceAppleScriptErrors = resolvedSurfaceAppleScriptErrors

    val activationService = PlatformActivationServiceDesktopImpl(
        surfaceAppleScriptErrors = resolvedSurfaceAppleScriptErrors,
    )

    val networkContentService = NetworkContentServiceDesktopImpl()
    val contentLoader = SpringboardContentLoaderDesktopImpl(networkContentService)

    // Shared Ktor client for any AI provider call (settings model fetch today; session chat tomorrow).
    val aiHttpClient = HttpClient(CIO)
    // Snapshot env vars once; the settings UI uses these to render the env-var-override label.
    val envVars: Map<String, String> = System.getenv()

    /**
     * Build a provider-specific [AiClient] using the API key the user supplied. The settings
     * UI calls this to populate its model dropdown; the session manager will call it the same
     * way once chat is wired up.
     */
    fun aiClientFor(provider: AiProvider, apiKey: String): AiClient? = when (provider) {
        AiProvider.OpenAi -> AiClientOpenAi(httpClient = aiHttpClient, apiKeyProvider = { apiKey })
        AiProvider.Anthropic -> AiClientAnthropic(httpClient = aiHttpClient, apiKeyProvider = { apiKey })
        AiProvider.None -> null
    }

    val aiFetchModels: suspend (AiProvider, String) -> List<com.strangeparticle.editio.client.AiClientModelInfo> =
        { provider, apiKey ->
            val client = aiClientFor(provider, apiKey)
            if (client == null) emptyList() else client.listModels(apiKey)
        }

    val startupTabs = settingsManager.getStringList(SettingsKey.STARTUP_TABS)

    println("[Springboard] startup tabs: ${if (startupTabs.isEmpty()) "none" else startupTabs.joinToString(", ")}")
    if (settingsManager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)) {
        println("[Springboard] AppleScript error surfacing enabled")
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(700.dp, 350.dp),
            position = WindowPosition(Alignment.Center)
        )

        val viewModel = remember {
            SpringboardViewModel(settingsManager, persistenceService, activationService, contentLoader)
        }
        val settingsViewModel = remember {
            SettingsViewModel(settingsManager = settingsManager)
        }
        val showSettings = remember { mutableStateOf(false) }
        val showActiveSettings = remember { mutableStateOf(false) }
        val showAssistant = remember { mutableStateOf(false) }
        val showLicenseDialog = remember { mutableStateOf(false) }
        val showNetworkDialog = remember { mutableStateOf(false) }
        val networkOpenIntoNewTab = remember { mutableStateOf(false) }
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
                hasActiveSpringboard = viewModel.springboard != null,
                canSaveActiveTabInPlace = viewModel.canSaveActiveTabInPlace,
                isActiveTabDirty = viewModel.activeTab?.isDirty == true,
                canCreateNewTab = viewModel.canCreateNewTab,
                onOpenInCurrentTab = {
                    val path = openFileDialog(null)
                    if (path != null) {
                        val contents = readFileContents(path)
                        if (contents != null) {
                            loadSpringboardConfig(path, contents)
                        } else {
                            viewModel.activeTabToast.error("Failed to open: file not found")
                        }
                    }
                },
                onOpenInNewTab = {
                    val path = openFileDialog(null)
                    if (path != null) {
                        val contents = readFileContents(path)
                        if (contents != null) {
                            viewModel.createTab()
                            loadSpringboardConfig(path, contents)
                        } else {
                            viewModel.activeTabToast.error("Failed to open: file not found")
                        }
                    }
                },
                onOpenFromNetworkInCurrentTab = {
                    networkOpenIntoNewTab.value = false
                    showNetworkDialog.value = true
                },
                onOpenFromNetworkInNewTab = {
                    networkOpenIntoNewTab.value = true
                    showNetworkDialog.value = true
                },
                onCopy = { sendMenuShortcut(KeyEvent.VK_C) },
                onPaste = { sendMenuShortcut(KeyEvent.VK_V) },
                onCloseCurrentTab = {
                    viewModel.closeTab(viewModel.activeTabId)
                },
                onPreviousTab = { viewModel.selectPreviousTab() },
                onNextTab = { viewModel.selectNextTab() },
                onSave = {
                    when (val result = viewModel.saveActiveTab()) {
                        is com.strangeparticle.springboard.app.viewmodel.SaveResult.Success ->
                            ToastBroadcaster.info("Saved to ${result.path}")
                        is com.strangeparticle.springboard.app.viewmodel.SaveResult.WriteFailed ->
                            ToastBroadcaster.error("Failed to save to ${result.path}: ${result.errorMessage}")
                        com.strangeparticle.springboard.app.viewmodel.SaveResult.NotSupportedForSource ->
                            ToastBroadcaster.error("Save is not supported for this source. Use Save a Local Copy As… instead.")
                        com.strangeparticle.springboard.app.viewmodel.SaveResult.NoSpringboard -> {
                            // Menu item should be disabled in this state — defensive no-op.
                        }
                    }
                },
                onSaveLocalCopyAs = {
                    val springboard = viewModel.springboard
                    if (springboard == null) {
                        return@SpringboardMenuBar
                    }
                    val suggestedName = springboard.name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "") + ".json"
                    val path = saveLocalCopyAsFileDialog(suggestedName)
                    if (path != null) {
                        when (val result = viewModel.saveActiveTabAs(path)) {
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.Success ->
                                ToastBroadcaster.info("Saved to ${result.path}")
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.WriteFailed ->
                                ToastBroadcaster.error("Failed to save to ${result.path}: ${result.errorMessage}")
                            com.strangeparticle.springboard.app.viewmodel.SaveResult.NotSupportedForSource ->
                                ToastBroadcaster.error("Save As is not supported for this tab.")
                            com.strangeparticle.springboard.app.viewmodel.SaveResult.NoSpringboard -> {
                                // No-op; menu only fires when there's a springboard.
                            }
                        }
                    }
                },
                onReload = {
                    if (viewModel.springboard == null) return@SpringboardMenuBar
                    kotlinx.coroutines.MainScope().launch {
                        viewModel.reloadCurrentSource()
                    }
                },
                onOpenSettings = openSettingsScreen,
                onShowActiveSettings = openActiveSettingsFromMain,
                onOpenAssistant = { showAssistant.value = true },
                onCloseAssistant = { showAssistant.value = false },
                onToggleAssistant = { showAssistant.value = !showAssistant.value },
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

            if (showNetworkDialog.value) {
                val intoNewTab = networkOpenIntoNewTab.value
                com.strangeparticle.springboard.app.ui.openbutton.OpenFromNetworkDialog(
                    onConfirm = { url ->
                        showNetworkDialog.value = false
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                val contents = networkContentService.fetchText(url)
                                if (intoNewTab) {
                                    viewModel.createTab()
                                }
                                loadSpringboardConfig(url, contents)
                            } catch (e: Exception) {
                                viewModel.activeTabToast.error("Failed to fetch: ${e.message}")
                            }
                        }
                    },
                    onDismiss = { showNetworkDialog.value = false },
                )
            }

            SpringboardApp(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                showSettings = showSettings,
                showActiveSettings = showActiveSettings,
                showAssistant = showAssistant,
                onOpenSettings = openSettingsScreen,
                onOpenActiveSettingsFromSettings = openActiveSettingsFromSettings,
                onCloseActiveSettings = closeActiveSettings,
                networkContentService = networkContentService,
                aiEnvironmentVariables = envVars,
                aiFetchModels = aiFetchModels,
                aiClientFactory = ::aiClientFor,
            )

            // Grow the window to fit the largest springboard across all tabs (never shrink).
            LaunchedEffect(viewModel.tabs.mapNotNull { it.springboard }) {
                growWindowToFitLargestTab(viewModel, windowState)
            }

            LaunchedEffect(Unit) {
                val tabRestorer = TabRestorer(persistenceService, contentLoader)
                tabRestorer.restoreInto(viewModel, startupTabs)

                println("[Springboard] application ready")
            }

            val windowInfo = LocalWindowInfo.current
            // Runs when the window focus state changes; when focus returns, restore keyboard focus.
            LaunchedEffect(windowInfo.isWindowFocused) {
                if (windowInfo.isWindowFocused && viewModel.isConfigLoaded) {
                    viewModel.requestFocusAppDropdown()
                }
            }
        }
    }
}

private fun sendMenuShortcut(keyCode: Int) {
    val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return
    val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
    val timestamp = System.currentTimeMillis()
    focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, timestamp, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED))
    focusOwner.dispatchEvent(KeyEvent(focusOwner, KeyEvent.KEY_RELEASED, timestamp, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED))
}
