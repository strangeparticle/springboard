package com.strangeparticle.springboard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar

@Composable
fun FrameWindowScope.SpringboardMenuBar(
    hasActiveSpringboard: Boolean,
    canSaveActiveTabInPlace: Boolean,
    isActiveTabDirty: Boolean,
    canCreateNewTab: Boolean,
    onOpenInCurrentTab: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onOpenFromNetworkInCurrentTab: () -> Unit,
    onOpenFromNetworkInNewTab: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onSave: () -> Unit,
    onSaveLocalCopyAs: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowActiveSettings: () -> Unit,
    onShowLicense: () -> Unit,
) {
    MenuBar {
        Menu("File") {
            // ── Open ────────────────────────────────────────────
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

            Separator()

            // ── Save ────────────────────────────────────────────
            // Save (in place) is disabled when the active tab is clean — there's nothing
            // to save when the in-memory model matches what's on disk / on the wire.
            // Save a Local Copy As… stays enabled whenever there is an active springboard;
            // it supports the "open then export a copy immediately" workflow on a clean tab.
            Item(
                "Save",
                enabled = canSaveActiveTabInPlace && isActiveTabDirty,
                shortcut = KeyShortcut(Key.S, meta = true),
            ) {
                onSave()
            }
            Item(
                "Save a Local Copy As…",
                enabled = hasActiveSpringboard,
                shortcut = KeyShortcut(Key.S, meta = true, shift = true),
            ) {
                onSaveLocalCopyAs()
            }

            Separator()

            // ── Reload ──────────────────────────────────────────
            Item("Reload", shortcut = KeyShortcut(Key.R, meta = true)) {
                onReload()
            }

            Separator()

            // ── Tab navigation ──────────────────────────────────
            Item("Previous Tab", shortcut = KeyShortcut(Key.LeftBracket, meta = true, shift = true)) {
                onPreviousTab()
            }
            Item("Next Tab", shortcut = KeyShortcut(Key.RightBracket, meta = true, shift = true)) {
                onNextTab()
            }

            Separator()

            // ── Close ───────────────────────────────────────────
            Item("Close Tab", shortcut = KeyShortcut(Key.W, meta = true)) {
                onCloseCurrentTab()
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
