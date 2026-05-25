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
    onCreateNewTab: () -> Unit,
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
            val fileMenuItems = springboardFileMenuItems(
                hasActiveSpringboard = hasActiveSpringboard,
                canSaveActiveTabInPlace = canSaveActiveTabInPlace,
                isActiveTabDirty = isActiveTabDirty,
                canCreateNewTab = canCreateNewTab,
                tabCount = tabCount,
                onCreateNewTab = onCreateNewTab,
                onOpenInCurrentTab = onOpenInCurrentTab,
                onOpenInNewTab = onOpenInNewTab,
                onOpenFromNetworkInCurrentTab = onOpenFromNetworkInCurrentTab,
                onOpenFromNetworkInNewTab = onOpenFromNetworkInNewTab,
                onOpenFromS3InCurrentTab = onOpenFromS3InCurrentTab,
                onOpenFromS3InNewTab = onOpenFromS3InNewTab,
                onCloseCurrentTab = onCloseCurrentTab,
                onPreviousTab = onPreviousTab,
                onNextTab = onNextTab,
                onSave = onSave,
                onSaveLocalCopyAs = onSaveLocalCopyAs,
                onReload = onReload,
            )

            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.TAB_CREATION }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
            }

            Separator()

            // ── Open ────────────────────────────────────────────
            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.OPEN }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
            }

            Separator()

            // ── Save ────────────────────────────────────────────
            // Save (in place) is disabled when the active tab is clean — there's nothing
            // to save when the in-memory model matches what's on disk / on the wire.
            // Save a Local Copy As… stays enabled whenever there is an active springboard;
            // it supports the "open then export a copy immediately" workflow on a clean tab.
            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.SAVE }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
            }

            Separator()

            // ── Reload ──────────────────────────────────────────
            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.RELOAD }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
            }

            Separator()

            // ── Tab navigation ──────────────────────────────────
            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.TAB_NAVIGATION }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
            }

            Separator()

            // ── Close ───────────────────────────────────────────
            for (fileMenuItem in fileMenuItems.filter { it.section == SpringboardFileMenuSection.CLOSE }) {
                Item(
                    fileMenuItem.label,
                    enabled = fileMenuItem.enabled,
                    shortcut = fileMenuItem.shortcut,
                ) {
                    fileMenuItem.onClick()
                }
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
