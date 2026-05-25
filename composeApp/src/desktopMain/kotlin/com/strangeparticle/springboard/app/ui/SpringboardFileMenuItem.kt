package com.strangeparticle.springboard.app.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut

data class SpringboardFileMenuItem(
    val section: SpringboardFileMenuSection,
    val label: String,
    val enabled: Boolean = true,
    val shortcut: KeyShortcut? = null,
    val onClick: () -> Unit,
)

enum class SpringboardFileMenuSection {
    TAB_CREATION,
    OPEN,
    SAVE,
    RELOAD,
    TAB_NAVIGATION,
    CLOSE,
}

fun springboardFileMenuItems(
    hasActiveSpringboard: Boolean,
    canSaveActiveTabInPlace: Boolean,
    isActiveTabDirty: Boolean,
    canCreateNewTab: Boolean,
    onCreateNewTab: () -> Unit,
    onOpenInCurrentTab: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onOpenFromNetworkInCurrentTab: () -> Unit,
    onOpenFromNetworkInNewTab: () -> Unit,
    onOpenFromS3InCurrentTab: () -> Unit,
    onOpenFromS3InNewTab: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onSave: () -> Unit,
    onSaveLocalCopyAs: () -> Unit,
    onReload: () -> Unit,
): List<SpringboardFileMenuItem> = listOf(
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.TAB_CREATION,
        label = "New Tab",
        enabled = canCreateNewTab,
        shortcut = KeyShortcut(Key.T, meta = true),
        onClick = onCreateNewTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from File in Current Tab…",
        shortcut = KeyShortcut(Key.O, meta = true),
        onClick = onOpenInCurrentTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from File in New Tab…",
        enabled = canCreateNewTab,
        shortcut = KeyShortcut(Key.O, meta = true, alt = true),
        onClick = onOpenInNewTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from Network in Current Tab…",
        shortcut = KeyShortcut(Key.O, meta = true, shift = true),
        onClick = onOpenFromNetworkInCurrentTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from Network in New Tab…",
        enabled = canCreateNewTab,
        shortcut = KeyShortcut(Key.O, meta = true, alt = true, shift = true),
        onClick = onOpenFromNetworkInNewTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from S3 in Current Tab…",
        shortcut = KeyShortcut(Key.O, meta = true, ctrl = true),
        onClick = onOpenFromS3InCurrentTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.OPEN,
        label = "Open from S3 in New Tab…",
        enabled = canCreateNewTab,
        shortcut = KeyShortcut(Key.O, meta = true, ctrl = true, shift = true),
        onClick = onOpenFromS3InNewTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.SAVE,
        label = "Save",
        enabled = canSaveActiveTabInPlace && isActiveTabDirty,
        shortcut = KeyShortcut(Key.S, meta = true),
        onClick = onSave,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.SAVE,
        label = "Save a Local Copy As…",
        enabled = hasActiveSpringboard,
        shortcut = KeyShortcut(Key.S, meta = true, shift = true),
        onClick = onSaveLocalCopyAs,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.RELOAD,
        label = "Reload",
        shortcut = KeyShortcut(Key.R, meta = true),
        onClick = onReload,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.TAB_NAVIGATION,
        label = "Previous Tab",
        shortcut = KeyShortcut(Key.LeftBracket, meta = true, shift = true),
        onClick = onPreviousTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.TAB_NAVIGATION,
        label = "Next Tab",
        shortcut = KeyShortcut(Key.RightBracket, meta = true, shift = true),
        onClick = onNextTab,
    ),
    SpringboardFileMenuItem(
        section = SpringboardFileMenuSection.CLOSE,
        label = "Close Tab",
        shortcut = KeyShortcut(Key.W, meta = true),
        onClick = onCloseCurrentTab,
    ),
)
