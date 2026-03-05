package com.strangeparticle.springboard.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.strangeparticle.springboard.app.platform.openFileDialog
import com.strangeparticle.springboard.app.platform.readFileContents
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import java.io.File

fun main(args: Array<String>) {
    println("[Springboard] platform initialized")

    val configPath = args.firstOrNull()
    println("[Springboard] config path: ${configPath ?: "none"}")

    application {
        val windowState = rememberWindowState(
            size = DpSize(700.dp, 250.dp),
            position = WindowPosition(Alignment.Center)
        )

        val viewModel = remember { SpringboardViewModel() }
        val environmentFocusRequester = remember { FocusRequester() }

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
                            println("[Springboard] config loading: $path")
                            viewModel.loadConfig(contents, path)
                            println("[Springboard] grid ready")
                            println("[Springboard] application ready")
                        }
                    }
                },
                onReload = {
                    val springboard = viewModel.springboard ?: return@SpringboardMenuBar
                    val path = springboard.source
                    val contents = readFileContents(path)
                    if (contents != null) {
                        println("[Springboard] config loading: $path")
                        viewModel.loadConfig(contents, path)
                        ToastBroadcaster.info("Springboard reloaded")
                    } else {
                        ToastBroadcaster.error("Failed to reload: file not found")
                    }
                }
            )

            SpringboardApp(
                viewModel = viewModel,
                environmentFocusRequester = environmentFocusRequester,
                onRequestFocusEnvironment = {
                    try {
                        environmentFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            )

            // Resize whenever the loaded springboard changes, regardless of how it was opened
            LaunchedEffect(viewModel.springboard) {
                resizeWindowToFitSpringboard(viewModel, windowState)
            }

            LaunchedEffect(configPath) {
                if (configPath != null) {
                    val file = File(configPath)
                    if (file.exists()) {
                        println("[Springboard] config loading: $configPath")
                        val contents = file.readText()
                        viewModel.loadConfig(contents, configPath)
                        println("[Springboard] grid ready")
                        println("[Springboard] application ready")
                    } else {
                        ToastBroadcaster.error("Config file not found: $configPath")
                        println("[Springboard] config file not found: $configPath")
                        println("[Springboard] application ready")
                    }
                } else {
                    println("[Springboard] application ready")
                }
            }

            val windowInfo = LocalWindowInfo.current
            LaunchedEffect(windowInfo.isWindowFocused) {
                if (windowInfo.isWindowFocused && viewModel.isConfigLoaded) {
                    // Small delay to ensure composition is ready before requesting focus
                    kotlinx.coroutines.delay(50)
                    try {
                        environmentFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

private fun resizeWindowToFitSpringboard(viewModel: SpringboardViewModel, windowState: WindowState) {
    val springboard = viewModel.springboard ?: return
    if (springboard.displayHints != null) {
        val width = springboard.displayHints.width
        val height = springboard.displayHints.height
        if (width != null && height != null) {
            windowState.size = DpSize(width.dp, height.dp)
            windowState.position = WindowPosition(Alignment.Center)
        }
        return
    }
    // Width: resource label (200) + app columns (180 each) + grid padding (32) + buffer (40)
    val width = 200 + (springboard.apps.size * 180) + 72
    // Height: navbar (56) + grid top padding (16) + env title (40) + header row (40)
    //         + rows (40 each) + grid bottom padding (16) + status bar (32) + buffer (40)
    val height = 56 + 16 + 40 + 40 + (springboard.resources.size * 40) + 16 + 32 + 40
    windowState.size = DpSize(width.dp, height.dp)
    windowState.position = WindowPosition(Alignment.Center)
}
