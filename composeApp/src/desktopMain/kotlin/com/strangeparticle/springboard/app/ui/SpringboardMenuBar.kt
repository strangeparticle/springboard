package com.strangeparticle.springboard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar

@Composable
fun FrameWindowScope.SpringboardMenuBar(
    onOpen: () -> Unit,
    onReload: () -> Unit
) {
    MenuBar {
        Menu("File") {
            Item("Open…", shortcut = KeyShortcut(Key.O, meta = true)) {
                onOpen()
            }
            Item("Reload", shortcut = KeyShortcut(Key.R, meta = true)) {
                onReload()
            }
        }
    }
}
