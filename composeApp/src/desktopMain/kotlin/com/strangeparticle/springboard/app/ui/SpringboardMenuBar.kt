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
    tabCount: Int,
    onOpenInCurrentTab: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onOpenFromNetworkInCurrentTab: () -> Unit,
    onOpenFromNetworkInNewTab: () -> Unit,
    onOpenFromS3InCurrentTab: () -> Unit,
    onOpenFromS3InNewTab: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onSave: () -> Unit,
    onSaveLocalCopyAs: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowActiveSettings: () -> Unit,
    onOpenAssistant: () -> Unit,
    onCloseAssistant: () -> Unit,
    onToggleAssistant: () -> Unit,
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
            Item("Open from S3 in Current Tab…", shortcut = KeyShortcut(Key.O, meta = true, ctrl = true)) {
                onOpenFromS3InCurrentTab()
            }
            Item(
                "Open from S3 in New Tab…",
                enabled = canCreateNewTab,
                shortcut = KeyShortcut(Key.O, meta = true, ctrl = true, shift = true),
            ) {
                onOpenFromS3InNewTab()
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
            val canNavigateTabs = tabCount > 1
            Item(
                "Previous Tab",
                enabled = canNavigateTabs,
                shortcut = KeyShortcut(Key.LeftBracket, meta = true, shift = true),
            ) {
                onPreviousTab()
            }
            Item(
                "Next Tab",
                enabled = canNavigateTabs,
                shortcut = KeyShortcut(Key.RightBracket, meta = true, shift = true),
            ) {
                onNextTab()
            }

            Separator()

            // ── Close ───────────────────────────────────────────
            Item("Close Tab", shortcut = KeyShortcut(Key.W, meta = true)) {
                onCloseCurrentTab()
            }
        }
        Menu("Edit") {
            Item("Copy", shortcut = KeyShortcut(Key.C, meta = true)) {
                onCopy()
            }
            Item("Paste", shortcut = KeyShortcut(Key.V, meta = true)) {
                onPaste()
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
        Menu("Assistant") {
            Item("Open Assistant", shortcut = KeyShortcut(Key.A, meta = true, shift = true)) {
                onOpenAssistant()
            }
            Item("Close Assistant") {
                onCloseAssistant()
            }
            Item("Toggle Assistant") {
                onToggleAssistant()
            }
        }
        Menu("Help") {
            Item("License…") {
                onShowLicense()
            }
        }
    }
}
