package com.strangeparticle.springboard.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.ComposeViewport
import com.strangeparticle.springboard.app.platform.NetworkContentServiceWasmImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.settings.persistence.SettingsPersistenceManagerWasm
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentWidthDp
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentHeightDp
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlin.math.min

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
    val persistenceManager = SettingsPersistenceManagerWasm()
    val settingsManager = SettingsManager(runtimeEnvironment, persistenceManager)
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
        val firstDropdownFocusRequester = remember { FocusRequester() }

        // Incremented each time the browser window gains focus
        var windowFocusTick by remember { mutableStateOf(0) }

        SpringboardApp(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            firstDropdownFocusRequester = firstDropdownFocusRequester,
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
                delay(50)
                try { firstDropdownFocusRequester.requestFocus() } catch (_: Exception) {}
            }
        }

        // Fetch and load config from the startup springboard URL
        LaunchedEffect(startupUrl) {
            if (startupUrl != null) {
                try {
                    val jsonText = networkContentService.fetchText(startupUrl)
                    viewModel.loadConfig(jsonText, startupUrl)
                    delay(300)
                    try { firstDropdownFocusRequester.requestFocus() } catch (_: Exception) {}
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

    // Subtract UI chrome from the viewport to get the space available for grid content.
    val navbarHeightDp = CommonUiConstants.NavbarHeight.value.toInt()
    val statusBarHeightDp = CommonUiConstants.StatusBarHeight.value.toInt()
    val activatorPreviewHeightDp = 19  // 11sp text + 2dp vertical padding * 2
    val availableWidth = viewportWidth
    val availableHeight = viewportHeight - navbarHeightDp - statusBarHeightDp - activatorPreviewHeightDp

    val naturalWidth = estimateGridContentWidthDp(springboard)
    val naturalHeight = estimateGridContentHeightDp(springboard)

    println("[Springboard] zoom fit: viewport=${viewportWidth}x${viewportHeight}, " +
        "available=${availableWidth}x${availableHeight}, " +
        "natural=${naturalWidth}x${naturalHeight}")

    if (naturalWidth <= 0 || naturalHeight <= 0) return

    val fitPercent = min(
        availableWidth * 100 / naturalWidth,
        availableHeight * 100 / naturalHeight,
    )

    val selected = GridZoomSelection.conservativePresetFor(fitPercent)
    println("[Springboard] zoom fit: fitPercent=$fitPercent, selected=${selected.percent}%")
    viewModel.gridZoomSelection = selected
}
