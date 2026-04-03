package com.strangeparticle.springboard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar

@Composable
fun FrameWindowScope.SpringboardMenuBar(
    hasActiveSpringboard: Boolean,
    onOpen: () -> Unit,
    onOpenFromNetwork: () -> Unit,
    onSaveLocalCopyAs: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowActiveSettings: () -> Unit,
    onShowLicense: () -> Unit,
) {
    MenuBar {
        Menu("File") {
            Item("Open from File…", shortcut = KeyShortcut(Key.O, meta = true)) {
                onOpen()
            }
            Item("Open from Network…", shortcut = KeyShortcut(Key.O, meta = true, shift = true)) {
                onOpenFromNetwork()
            }
            Item(
                "Save a Local Copy As…",
                enabled = hasActiveSpringboard,
                shortcut = KeyShortcut(Key.S, meta = true, shift = true)
            ) {
                onSaveLocalCopyAs()
            }
            Item("Reload", shortcut = KeyShortcut(Key.R, meta = true)) {
                onReload()
            }
        }
        Menu("Settings") {
            Item("Settings…", shortcut = KeyShortcut(Key.Comma, meta = true)) {
                onOpenSettings()
            }
            Item("Show Active Settings") {
                onShowActiveSettings()
            }
        }
        Menu("Help") {
            Item("License…") {
                onShowLicense()
            }
        }
    }
}
