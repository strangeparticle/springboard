package com.strangeparticle.springboard.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.ComposeViewport
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.detectRuntimeEnvironment
import com.strangeparticle.springboard.app.settings.persistence.WasmSettingsPersistenceManager
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private fun getConfigUrl(): JsAny? = js("window.springboardConfigUrl")

// Returns a JS Promise (as JsAny) that resolves to the response text
@JsFun("(url) => fetch(url).then(r => r.ok ? r.text() : Promise.reject('HTTP ' + r.status))")
private external fun fetchAsPromise(url: String): JsAny

// Attaches resolve/reject callbacks to a Promise
@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(e => reject(typeof e === 'string' ? e : (e.message ?? String(e))))")
private external fun promiseThen(
    promise: JsAny,
    resolve: (JsString) -> Unit,
    reject: (JsString) -> Unit
)

// Registers a callback for the browser window gaining focus
@JsFun("(callback) => window.addEventListener('focus', callback)")
private external fun addWindowFocusListener(callback: () -> Unit)

private suspend fun fetchText(url: String): String = suspendCancellableCoroutine { continuation ->
    promiseThen(
        promise = fetchAsPromise(url),
        resolve = { text -> continuation.resume(text.toString()) },
        reject = { error -> continuation.resumeWithException(Exception(error.toString())) }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val runtimeEnvironment = detectRuntimeEnvironment()
    val persistenceManager = WasmSettingsPersistenceManager()
    val settingsManager = SettingsManager(runtimeEnvironment, persistenceManager)
    settingsManager.loadSettingsAtStartup()

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
            firstDropdownFocusRequester = firstDropdownFocusRequester
        )

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

        // Fetch and load config from the URL set in index.html
        LaunchedEffect(Unit) {
            val configUrl = getConfigUrl()
            val urlString = configUrl?.toString()
            if (urlString != null && urlString != "undefined" && urlString.isNotBlank()) {
                try {
                    val jsonText = fetchText(urlString)
                    viewModel.loadConfig(jsonText, urlString)
                    // Wait for Compose to recompose and render the dropdowns before requesting focus
                    delay(300)
                    try { firstDropdownFocusRequester.requestFocus() } catch (_: Exception) {}
                } catch (e: Throwable) {
                    ToastBroadcaster.error("Failed to fetch config: ${e.message}")
                }
            }
        }
    }
}
