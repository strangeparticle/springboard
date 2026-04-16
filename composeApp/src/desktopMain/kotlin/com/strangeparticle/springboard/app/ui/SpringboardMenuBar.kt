package com.strangeparticle.springboard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar

@Composable
fun FrameWindowScope.SpringboardMenuBar(
    hasActiveSpringboard: Boolean,
    canCreateNewTab: Boolean,
    onOpenInCurrentTab: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onOpenFromNetworkInCurrentTab: () -> Unit,
    onOpenFromNetworkInNewTab: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onSaveLocalCopyAs: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowActiveSettings: () -> Unit,
    onShowLicense: () -> Unit,
) {
    MenuBar {
        Menu("File") {
            Item("Open from File in Current Tab…", shortcut = KeyShortcut(Key.O, meta = true)) {
                onOpenInCurrentTab()
            }
            Item(
                "Open from File in New Tab…",
                enabled = canCreateNewTab,
                shortcut = KeyShortcut(Key.O, meta = true, alt = true),
            ) {
                onOpenInNewTab()
            }
            Item("Open from Network in Current Tab…", shortcut = KeyShortcut(Key.O, meta = true, shift = true)) {
                onOpenFromNetworkInCurrentTab()
            }
            Item(
                "Open from Network in New Tab…",
                enabled = canCreateNewTab,
                shortcut = KeyShortcut(Key.O, meta = true, alt = true, shift = true),
            ) {
                onOpenFromNetworkInNewTab()
            }
            Item("Close Tab", shortcut = KeyShortcut(Key.W, meta = true)) {
                onCloseCurrentTab()
            }
            Item("Previous Tab", shortcut = KeyShortcut(Key.LeftBracket, meta = true, shift = true)) {
                onPreviousTab()
            }
            Item("Next Tab", shortcut = KeyShortcut(Key.RightBracket, meta = true, shift = true)) {
                onNextTab()
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
