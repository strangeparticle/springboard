package com.strangeparticle.springboard.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.strangeparticle.springboard.app.persistence.PersistenceServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.NetworkContentServiceWasmImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.gridnav.computeAvailableGridArea
import com.strangeparticle.springboard.app.ui.gridnav.computeZoomToFit
import com.strangeparticle.springboard.app.ui.gridnav.displayLabel
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.browser.document

@JsFun("() => window.startupSpringboard")
private external fun getStartupSpringboard(): JsAny?

@JsFun("() => window.activeBrand")
private external fun getActiveBrand(): JsAny?

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
    // Read JS globals and forward them to the settings manager as env-var overrides.
    val startupSpringboard = getStartupSpringboard()?.toString()
        ?.takeIf { it != "undefined" && it != "null" && it.isNotBlank() }
    val activeBrand = getActiveBrand()?.toString()
        ?.takeIf { it != "undefined" && it != "null" && it.isNotBlank() }

    val environmentVariables = buildMap {
        if (startupSpringboard != null) {
            put("SPRINGBOARD_STARTUP_SPRINGBOARD", startupSpringboard)
        }
        if (activeBrand != null) {
            put("SPRINGBOARD_ACTIVE_BRAND", activeBrand)
        }
    }

    settingsManager.loadSettingsAtStartup(environmentVariables = environmentVariables)

    val networkContentService = NetworkContentServiceWasmImpl()
    val startupUrl = settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)?.path

    ComposeViewport(document.body!!) {
        val viewModel = remember { SpringboardViewModel(settingsManager) }
        val settingsViewModel = remember {
            SettingsViewModel(
                settingsManager = settingsManager,
                currentFilePath = { viewModel.springboard?.source },
            )
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

        // Fetch and load config from the startup springboard URL
        LaunchedEffect(startupUrl) {
            if (startupUrl != null) {
                try {
                    val jsonText = networkContentService.fetchText(startupUrl)
                    viewModel.loadConfig(jsonText, startupUrl)
                } catch (e: Throwable) {
                    ToastBroadcaster.error("Failed to fetch config: ${e.message}")
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
