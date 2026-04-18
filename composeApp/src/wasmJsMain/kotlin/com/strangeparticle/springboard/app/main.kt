package com.strangeparticle.springboard.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.strangeparticle.springboard.app.persistence.PersistenceServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.NetworkContentServiceWasmImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.settings.parseUrlParamsAsCommandLineArgs
import com.strangeparticle.springboard.app.settings.readJsGlobalsAsEnvironmentVariables
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.gridnav.computeAvailableGridArea
import com.strangeparticle.springboard.app.ui.gridnav.computeZoomToFit
import com.strangeparticle.springboard.app.ui.gridnav.displayLabel
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoaderWasmImpl
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.viewmodel.TabRestorer
import kotlinx.browser.document

@JsFun("(callback) => window.addEventListener('focus', callback)")
private external fun addWindowFocusListener(callback: () -> Unit)

@JsFun("() => window.innerWidth")
private external fun getWindowInnerWidth(): Int

@JsFun("() => window.innerHeight")
private external fun getWindowInnerHeight(): Int

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val runtimeEnvironment = detectRuntimeEnvironment()
    val persistenceService = PersistenceServiceDefaultImpl()
    val settingsManager = SettingsManager(runtimeEnvironment, persistenceService)
    val environmentVariables = readJsGlobalsAsEnvironmentVariables()
    val urlParamArgs = parseUrlParamsAsCommandLineArgs()
    settingsManager.loadSettingsAtStartup(
        environmentVariables = environmentVariables,
        commandLineArgs = urlParamArgs,
    )

    val networkContentService = NetworkContentServiceWasmImpl()
    val startupTabs = settingsManager.getStringList(SettingsKey.STARTUP_TABS)

    ComposeViewport(document.body!!) {
        val viewModel = remember { SpringboardViewModel(settingsManager, persistenceService) }
        val settingsViewModel = remember {
            SettingsViewModel(settingsManager = settingsManager)
        }
        // Incremented each time the browser window gains focus
        var windowFocusTick by remember { mutableStateOf(0) }

        SpringboardApp(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            networkContentService = networkContentService,
            showFileOpen = false,
        )

        // Auto-select a conservative zoom preset when a springboard is first loaded.
        // Keyed on isConfigLoaded so it fires once (stays true across reloads).
        LaunchedEffect(viewModel.isConfigLoaded) {
            if (viewModel.isConfigLoaded) {
                selectZoomToFitViewport(viewModel)
            }
        }

        // Register the JS window focus listener once
        LaunchedEffect(Unit) {
            addWindowFocusListener { windowFocusTick++ }
        }

        // Re-focus the first dropdown whenever the window regains focus
        LaunchedEffect(windowFocusTick) {
            if (windowFocusTick > 0 && viewModel.isConfigLoaded) {
                viewModel.requestFocusAppDropdown()
            }
        }

        LaunchedEffect(Unit) {
            val contentLoader = SpringboardContentLoaderWasmImpl(networkContentService)
            val tabRestorer = TabRestorer(persistenceService, contentLoader)
            val hadPersistedTabs = persistenceService.loadTabs() != null
            if (hadPersistedTabs) {
                tabRestorer.restoreInto(viewModel)
            } else if (startupTabs.isNotEmpty()) {
                for ((index, tabSource) in startupTabs.withIndex()) {
                    if (index > 0) viewModel.createTab()
                    try {
                        val jsonText = networkContentService.fetchText(tabSource)
                        viewModel.loadConfig(jsonText, tabSource)
                    } catch (e: Throwable) {
                        viewModel.activeTabToast.error("Failed to fetch config: ${e.message}")
                    }
                }
            }
        }
    }
}

/**
 * Reads the browser viewport size, estimates how large the grid needs to be at 100%,
 * and picks the highest zoom preset that is conservatively below the calculated fit.
 */
private fun selectZoomToFitViewport(viewModel: SpringboardViewModel) {
    val springboard = viewModel.springboard ?: run {
        println("[Springboard] selectZoomToFitViewport: no springboard loaded")
        return
    }

    val viewportWidth = getWindowInnerWidth()
    val viewportHeight = getWindowInnerHeight()
    val (availableWidth, availableHeight) = computeAvailableGridArea(viewportWidth, viewportHeight)

    println("[Springboard] zoom fit: viewport=${viewportWidth}x${viewportHeight}, " +
        "available=${availableWidth}x${availableHeight}")

    val selected = computeZoomToFit(availableWidth, availableHeight, springboard)
    println("[Springboard] zoom fit: selected=${selected.displayLabel()}")
    viewModel.gridZoomSelection = selected
}
