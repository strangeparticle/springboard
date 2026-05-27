package com.strangeparticle.springboard.app.unit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.window.FrameWindowScope
import com.strangeparticle.springboard.app.ui.SpringboardMenuBar
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JSeparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class SpringboardMenuBarTest {

    @Test
    fun fileMenuGroupsLogicalItemsWithSeparators() = runDesktopComposeUiTest {
        val window = runOnUiThread {
            ComposeWindow().apply {
                setContent { renderMenuBar(tabCount = 2) }
                isVisible = true
            }
        }
        try {
            waitUntil("menu bar is installed") {
                runOnUiThread {
                    window.jMenuBar != null
                }
            }

            runOnUiThread {
                val fileMenu = assertNotNull(window.jMenuBar.getMenuWithText("File"))

                assertEquals(
                    listOf(
                        "New Tab",
                        "---",
                        "Open from File in Current Tab…",
                        "Open from File in New Tab…",
                        "Open from Network in Current Tab…",
                        "Open from Network in New Tab…",
                        "Open from S3 in Current Tab…",
                        "Open from S3 in New Tab…",
                        "---",
                        "Save",
                        "Save a Local Copy As…",
                        "---",
                        "Reload",
                        "---",
                        "Previous Tab",
                        "Next Tab",
                        "---",
                        "Close Tab",
                    ),
                    fileMenu.componentLabels(),
                )
            }
        } finally {
            runOnUiThread { window.dispose() }
        }
    }

    @Test
    fun singleTabDisablesPreviousAndNextTabMenuItems() = runDesktopComposeUiTest {
        val window = runOnUiThread {
            ComposeWindow().apply {
                setContent { renderMenuBar(tabCount = 1) }
                isVisible = true
            }
        }
        try {
            waitUntil("menu bar is installed") {
                runOnUiThread {
                    window.jMenuBar != null
                }
            }

            runOnUiThread {
                val fileMenu = assertNotNull(window.jMenuBar.getMenuWithText("File"))
                assertFalse(fileMenu.getItemWithText("Previous Tab").isEnabled)
                assertFalse(fileMenu.getItemWithText("Next Tab").isEnabled)
            }
        } finally {
            runOnUiThread { window.dispose() }
        }
    }

    @Composable
    private fun FrameWindowScope.renderMenuBar(tabCount: Int) {
        SpringboardMenuBar(
            hasActiveSpringboard = true,
            canSaveActiveTabInPlace = false,
            isActiveTabDirty = false,
            canCreateNewTab = true,
            onCreateNewTab = {},
            tabCount = tabCount,
            onOpenInCurrentTab = {},
            onOpenInNewTab = {},
            onOpenFromNetworkInCurrentTab = {},
            onOpenFromNetworkInNewTab = {},
            onOpenFromS3InCurrentTab = {},
            onOpenFromS3InNewTab = {},
            onCopy = {},
            onPaste = {},
            onCloseCurrentTab = {},
            onPreviousTab = {},
            onNextTab = {},
            onSave = {},
            onSaveLocalCopyAs = {},
            onReload = {},
            onOpenSettings = {},
            onShowActiveSettings = {},
            onOpenAssistant = {},
            onCloseAssistant = {},
            onToggleAssistant = {},
            onShowLicense = {},
        )
    }

    private fun javax.swing.JMenuBar.getMenuWithText(text: String): JMenu? {
        for (index in 0 until menuCount) {
            val menu = getMenu(index)
            if (menu.text == text) {
                return menu
            }
        }
        return null
    }

    private fun JMenu.componentLabels(): List<String> =
        menuComponents.map { component ->
            when (component) {
                is JMenuItem -> component.text
                is JSeparator -> "---"
                else -> error("Unexpected File menu component: ${component::class.qualifiedName}")
            }
        }

    private fun JMenu.getItemWithText(text: String): JMenuItem {
        for (index in 0 until itemCount) {
            val menuItem = getItem(index)
            if (menuItem?.text == text) {
                return menuItem
            }
        }
        error("Menu item not found: $text")
    }
}
