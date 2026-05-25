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
import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.aws.AwsCliCredentialProvider
import com.strangeparticle.springboard.app.persistence.PersistenceServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.*
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.settings.items.core.HideAppAfterActivationSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpContentTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.OpenFromS3AwsProfileSetting
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import com.strangeparticle.springboard.app.settings.items.core.SurfaceAppleScriptErrorsSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import com.strangeparticle.springboard.app.ui.dialog.LicenseDialog
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoaderDesktopImpl
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.viewmodel.TabRestorer
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
    val settingsRegistry = SettingsRegistry(
        coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() }
    )
    val settingsManager = SettingsManager(runtimeEnvironment, settingsRegistry, persistenceService)

    // Load all settings sources: persisted user settings, env vars, CLI args
    settingsManager.loadSettingsAtStartup(
        environmentVariables = System.getenv(),
        commandLineArgs = args.toList(),
    )

    // Propagate the resolved surfaceAppleScriptErrors value to the browser automation module
    val resolvedSurfaceAppleScriptErrors = settingsManager.resolveValue(SurfaceAppleScriptErrorsSetting)
    surfaceAppleScriptErrors = resolvedSurfaceAppleScriptErrors

    val activationService = PlatformActivationServiceDesktopImpl(
        surfaceAppleScriptErrors = resolvedSurfaceAppleScriptErrors,
    )

    val contentTimeoutSeconds = settingsManager.resolveValue(HttpContentTimeoutSecondsSetting)
    val aiProviderTimeoutSeconds = settingsManager.resolveValue(HttpAiProviderTimeoutSecondsSetting)
    val contentHttpClient = HttpClientFactory.createCioClient(
        HttpClientTimeoutConfig.fromSeconds(contentTimeoutSeconds),
    )
    val aiHttpClient = HttpClientFactory.createCioClient(
        HttpClientTimeoutConfig.fromSeconds(aiProviderTimeoutSeconds),
    )

    val networkContentService = NetworkContentServiceDesktopImpl(contentHttpClient)
    val contentLoader = SpringboardContentLoaderDesktopImpl(networkContentService)

    // Shared Ktor client for any AI provider call. Plugged into the SettingsViewModel
    // so DropDownFromApiCallSettingsItem.loadOptions and AiProvider.createClient can
    // reach it through the standard SettingsItemContext.
    val awsCredentialProvider = AwsCliCredentialProvider()
    val s3ContentService = S3ContentServiceDesktopImpl(contentHttpClient, awsCredentialProvider)

    val startupTabs = settingsManager.resolveValue(StartupTabsSetting)

    println("[Springboard] startup tabs: ${if (startupTabs.isEmpty()) "none" else startupTabs.joinToString(", ")}")
    if (resolvedSurfaceAppleScriptErrors) {
        println("[Springboard] AppleScript error surfacing enabled")
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(700.dp, 350.dp),
            position = WindowPosition(Alignment.Center)
        )

        val viewModel = remember {
            SpringboardViewModel(
                settingsManager = settingsManager,
                persistenceService = persistenceService,
                platformActivationService = activationService,
                contentLoader = contentLoader,
                s3ContentService = s3ContentService,
            )
        }
        val settingsViewModel = remember {
            SettingsViewModel(settingsManager = settingsManager, httpClient = aiHttpClient)
        }
        val showSettings = remember { mutableStateOf(false) }
        val showActiveSettings = remember { mutableStateOf(false) }
        val showAssistant = remember { mutableStateOf(false) }
        val showLicenseDialog = remember { mutableStateOf(false) }
        val showNetworkDialog = remember { mutableStateOf(false) }
        val networkOpenIntoNewTab = remember { mutableStateOf(false) }
        val showS3Dialog = remember { mutableStateOf(false) }
        val s3OpenIntoNewTab = remember { mutableStateOf(false) }
        val s3ConflictSourceUrl = remember { mutableStateOf<String?>(null) }
        val s3ConflictTabId = remember { mutableStateOf<String?>(null) }
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
                hasActiveSpringboard = viewModel.springboardFilteredForRuntime != null,
                canSaveActiveTabInPlace = viewModel.canSaveActiveTabInPlace,
                isActiveTabDirty = viewModel.activeTab?.isDirty == true,
                canCreateNewTab = viewModel.canCreateNewTab,
                onCreateNewTab = { viewModel.createTab() },
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
                onOpenFromS3InCurrentTab = {
                    s3OpenIntoNewTab.value = false
                    showS3Dialog.value = true
                },
                onOpenFromS3InNewTab = {
                    s3OpenIntoNewTab.value = true
                    showS3Dialog.value = true
                },
                onCopy = { sendMenuShortcut(KeyEvent.VK_C) },
                onPaste = { sendMenuShortcut(KeyEvent.VK_V) },
                onCloseCurrentTab = {
                    viewModel.closeTab(viewModel.activeTabId)
                },
                onPreviousTab = { viewModel.selectPreviousTab() },
                onNextTab = { viewModel.selectNextTab() },
                onSave = {
                    val tabId = viewModel.activeTabId
                    kotlinx.coroutines.MainScope().launch {
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
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.Conflict -> {
                                s3ConflictSourceUrl.value = result.sourceUrl
                                s3ConflictTabId.value = tabId
                            }
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.Denied ->
                                ToastBroadcaster.error("Save failed — your AWS profile doesn't have write permission. ${result.message}")
                        }
                    }
                },
                onSaveLocalCopyAs = {
                    val springboardFilteredForRuntime = viewModel.springboardFilteredForRuntime
                    if (springboardFilteredForRuntime == null) {
                        return@SpringboardMenuBar
                    }
                    val suggestedName = springboardFilteredForRuntime.name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "") + ".json"
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
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.Conflict,
                            is com.strangeparticle.springboard.app.viewmodel.SaveResult.Denied -> {
                                // Save As writes to a local path; S3-specific outcomes don't apply here.
                            }
                        }
                    }
                },
                onReload = {
                    if (viewModel.springboardFilteredForRuntime == null) return@SpringboardMenuBar
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

            if (showS3Dialog.value) {
                val intoNewTab = s3OpenIntoNewTab.value
                val defaultAwsProfile = settingsManager.resolveValue(OpenFromS3AwsProfileSetting)
                com.strangeparticle.springboard.app.ui.openbutton.OpenFromS3Dialog(
                    defaultAwsProfile = defaultAwsProfile,
                    onConfirm = { request ->
                        showS3Dialog.value = false
                        kotlinx.coroutines.MainScope().launch {
                            val outcome = viewModel.loadConfigFromS3(
                                url = request.url,
                                profile = request.awsProfile,
                                inNewTab = intoNewTab,
                            )
                            if (outcome is com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel.LoadResult.Failure) {
                                viewModel.activeTabToast.error(outcome.message)
                            }
                        }
                    },
                    onDismiss = { showS3Dialog.value = false },
                )
            }

            val conflictUrl = s3ConflictSourceUrl.value
            val conflictTabId = s3ConflictTabId.value
            if (conflictUrl != null && conflictTabId != null) {
                com.strangeparticle.springboard.app.ui.openbutton.S3ConflictDialog(
                    sourceUrl = conflictUrl,
                    onDismiss = {
                        s3ConflictSourceUrl.value = null
                        s3ConflictTabId.value = null
                    },
                    onOverwrite = {
                        s3ConflictSourceUrl.value = null
                        s3ConflictTabId.value = null
                        kotlinx.coroutines.MainScope().launch {
                            when (val result = viewModel.saveTabOverwriting(conflictTabId)) {
                                is com.strangeparticle.springboard.app.viewmodel.SaveResult.Success ->
                                    ToastBroadcaster.info("Saved to ${result.path}")
                                is com.strangeparticle.springboard.app.viewmodel.SaveResult.WriteFailed ->
                                    ToastBroadcaster.error("Failed to save to ${result.path}: ${result.errorMessage}")
                                is com.strangeparticle.springboard.app.viewmodel.SaveResult.Denied ->
                                    ToastBroadcaster.error("Save failed — your AWS profile doesn't have write permission. ${result.message}")
                                else -> {
                                    // Other variants shouldn't appear here for an S3-backed tab.
                                }
                            }
                        }
                    },
                    onReload = {
                        val tab = viewModel.findTab(conflictTabId)
                        val profile = tab?.s3AwsProfile
                        val source = tab?.source
                        s3ConflictSourceUrl.value = null
                        s3ConflictTabId.value = null
                        if (profile != null && source != null) {
                            kotlinx.coroutines.MainScope().launch {
                                viewModel.loadConfigFromS3(url = source, profile = profile, inNewTab = false)
                            }
                        }
                    },
                    onSaveAs = {
                        val tabIdForSaveAs = conflictTabId
                        s3ConflictSourceUrl.value = null
                        s3ConflictTabId.value = null
                        val springboardFilteredForRuntime =
                            viewModel.findTab(tabIdForSaveAs)?.springboardFilteredForRuntime
                        if (springboardFilteredForRuntime != null) {
                            val suggestedName = springboardFilteredForRuntime.name
                                .replace(Regex("[^a-zA-Z0-9._\\- ]"), "") + ".json"
                            val path = saveLocalCopyAsFileDialog(suggestedName)
                            if (path != null) {
                                when (val result = viewModel.saveActiveTabAs(path)) {
                                    is com.strangeparticle.springboard.app.viewmodel.SaveResult.Success ->
                                        ToastBroadcaster.info("Saved to ${result.path}")
                                    is com.strangeparticle.springboard.app.viewmodel.SaveResult.WriteFailed ->
                                        ToastBroadcaster.error("Failed to save to ${result.path}: ${result.errorMessage}")
                                    else -> {
                                        // No-op for non-applicable variants.
                                    }
                                }
                            }
                        }
                    },
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
            )

            LaunchedEffect(viewModel.tabs.mapNotNull { it.springboardFilteredForRuntime }) {
                if (!viewModel.suppressWindowGrow) {
                    growWindowToFitLargestTab(viewModel, windowState)
                }
            }

            LaunchedEffect(Unit) {
                val tabRestorer = TabRestorer(
                    persistenceService = persistenceService,
                    loader = contentLoader,
                    s3ContentService = s3ContentService,
                )
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
