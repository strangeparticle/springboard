package com.strangeparticle.springboard.app.unit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import com.strangeparticle.springboard.app.ui.springboardFileMenuItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpringboardFileMenuItemsTest {

    @Test
    fun newTabMenuItemExistsIsEnabledAndCreatesNewTab() = runComposeUiTest {
        var createNewTabCount = 0

        val newTabItem = defaultFileMenuItems(
            canCreateNewTab = true,
            onCreateNewTab = { createNewTabCount += 1 },
        ).single { it.label == "New Tab" }

        assertTrue(newTabItem.enabled)
        assertEquals(KeyShortcut(Key.T, meta = true), newTabItem.shortcut)
        newTabItem.onClick()

        assertEquals(1, createNewTabCount)
    }

    @Test
    fun newTabMenuItemIsDisabledWhenTabsAreAtLimit() = runComposeUiTest {
        val newTabItem = defaultFileMenuItems(canCreateNewTab = false)
            .single { it.label == "New Tab" }

        assertFalse(newTabItem.enabled)
    }

    @Test
    fun tabNavigationMenuItemsAreDisabledWithOneTab() = runComposeUiTest {
        val tabNavigationItems = defaultFileMenuItems(tabCount = 1)
            .filter { it.label == "Previous Tab" || it.label == "Next Tab" }

        assertEquals(2, tabNavigationItems.size)
        assertTrue(tabNavigationItems.all { !it.enabled })
    }

    @Test
    fun tabNavigationMenuItemsAreEnabledWithMultipleTabs() = runComposeUiTest {
        val tabNavigationItems = defaultFileMenuItems(tabCount = 2)
            .filter { it.label == "Previous Tab" || it.label == "Next Tab" }

        assertEquals(2, tabNavigationItems.size)
        assertTrue(tabNavigationItems.all { it.enabled })
    }

    private fun defaultFileMenuItems(
        canCreateNewTab: Boolean = true,
        tabCount: Int = 2,
        onCreateNewTab: () -> Unit = {},
    ) = springboardFileMenuItems(
        hasActiveSpringboard = true,
        canSaveActiveTabInPlace = true,
        isActiveTabDirty = true,
        canCreateNewTab = canCreateNewTab,
        tabCount = tabCount,
        onCreateNewTab = onCreateNewTab,
        onOpenInCurrentTab = {},
        onOpenInNewTab = {},
        onOpenFromNetworkInCurrentTab = {},
        onOpenFromNetworkInNewTab = {},
        onOpenFromS3InCurrentTab = {},
        onOpenFromS3InNewTab = {},
        onCloseCurrentTab = {},
        onPreviousTab = {},
        onNextTab = {},
        onSave = {},
        onSaveLocalCopyAs = {},
        onReload = {},
    )
}
