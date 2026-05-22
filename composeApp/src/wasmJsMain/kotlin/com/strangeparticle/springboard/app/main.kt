package com.strangeparticle.springboard.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.persistence.PersistenceServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.NetworkContentServiceWasmImpl
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import com.strangeparticle.springboard.app.settings.parseUrlParams
import com.strangeparticle.springboard.app.settings.readJsGlobalsAsEnvironmentVariables
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.gridnav.computeAvailableGridArea
import com.strangeparticle.springboard.app.ui.gridnav.computeZoomToFit
import com.strangeparticle.springboard.app.ui.gridnav.displayLabel
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoaderWasmImpl
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.app.viewmodel.TabRestorer
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
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
    val settingsRegistry = SettingsRegistry(
        coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() }
    )
    val settingsManager = SettingsManager(runtimeEnvironment, settingsRegistry, persistenceService)
    val environmentVariables = readJsGlobalsAsEnvironmentVariables()
    val urlParams = parseUrlParams(settingsRegistry)
    settingsManager.loadSettingsAtStartup(
        environmentVariables = environmentVariables,
        urlParams = urlParams,
    )

    val networkContentService = NetworkContentServiceWasmImpl()
    val contentLoader = SpringboardContentLoaderWasmImpl(networkContentService)
    val aiHttpClient = HttpClient(Js)
    val startupTabs = settingsManager.resolveValue(StartupTabsSetting)

    ComposeViewport(document.body!!) {
        val viewModel = remember {
            SpringboardViewModel(
                settingsManager = settingsManager,
                persistenceService = persistenceService,
                contentLoader = contentLoader,
            )
        }
        val settingsViewModel = remember {
            SettingsViewModel(settingsManager = settingsManager, httpClient = aiHttpClient)
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
            val tabRestorer = TabRestorer(persistenceService, contentLoader)
            tabRestorer.restoreInto(viewModel, startupTabs)
        }
    }
}

/**
 * Reads the browser viewport size, estimates how large the grid needs to be at 100%,
 * and picks the highest zoom preset that is conservatively below the calculated fit.
 */
private fun selectZoomToFitViewport(viewModel: SpringboardViewModel) {
    val springboardFilteredForRuntime = viewModel.springboardFilteredForRuntime ?: run {
        println("[Springboard] selectZoomToFitViewport: no springboard loaded")
        return
    }

    val viewportWidth = getWindowInnerWidth()
    val viewportHeight = getWindowInnerHeight()
    val (availableWidth, availableHeight) = computeAvailableGridArea(viewportWidth, viewportHeight)

    println("[Springboard] zoom fit: viewport=${viewportWidth}x${viewportHeight}, " +
        "available=${availableWidth}x${availableHeight}")

    val selected = computeZoomToFit(availableWidth, availableHeight, springboardFilteredForRuntime)
    println("[Springboard] zoom fit: selected=${selected.displayLabel()}")
    viewModel.gridZoomSelection = selected
}
