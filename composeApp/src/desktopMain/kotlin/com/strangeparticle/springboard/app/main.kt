package com.strangeparticle.springboard.app

import androidx.compose.runtime.LaunchedEffect
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
import com.strangeparticle.springboard.app.platform.surfaceAppleScriptErrors
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import java.io.File

private data class LaunchArgs(
    val configPath: String?,
    val surfaceAppleScriptErrors: Boolean,
)

private fun parseLaunchArgs(args: Array<String>): LaunchArgs {
    val surfaceErrors = "--surface-applescript-errors" in args
    val configPath = args.filterNot { it.startsWith("--") }.firstOrNull()
    return LaunchArgs(configPath = configPath, surfaceAppleScriptErrors = surfaceErrors)
}

fun main(args: Array<String>) {
    println("[Springboard] platform initialized")

    val launchArgs = parseLaunchArgs(args)

    surfaceAppleScriptErrors = launchArgs.surfaceAppleScriptErrors
    if (surfaceAppleScriptErrors) println("[Springboard] AppleScript error surfacing enabled")

    val configPath = launchArgs.configPath
    println("[Springboard] launch config path: ${configPath ?: "none"}")

    application {
        val windowState = rememberWindowState(
            size = DpSize(700.dp, 250.dp),
            position = WindowPosition(Alignment.Center)
        )

        val viewModel = remember { SpringboardViewModel() }
        val firstDropdownFocusRequester = remember { FocusRequester() }
        val loadSpringboardConfig: (String, String) -> Unit = { path, contents ->
            println("[Springboard] config loading: $path")
            viewModel.loadConfig(contents, path)
            println("[Springboard] grid ready")
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
                onReload = {
                    val springboard = viewModel.springboard ?: return@SpringboardMenuBar
                    val path = springboard.source
                    val contents = readFileContents(path)
                    if (contents != null) {
                        loadSpringboardConfig(path, contents)
                        ToastBroadcaster.info("Springboard reloaded")
                    } else {
                        ToastBroadcaster.error("Failed to reload: file not found")
                    }
                }
            )

            SpringboardApp(
                viewModel = viewModel,
                firstDropdownFocusRequester = firstDropdownFocusRequester,
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
