package com.strangeparticle.springboard.app.unit.ui.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.tabs.TabBar
import com.strangeparticle.springboard.app.viewmodel.MAX_OPEN_TABS
import com.strangeparticle.springboard.app.viewmodel.TabState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TabBarTest {

    private fun makeTabs(count: Int): List<TabState> =
        (1..count).map { TabState.createEmpty("tab-$it").copy(label = "Label $it") }

    @Test
    fun allTabsRender() = runComposeUiTest {
        val tabs = makeTabs(3)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
            TabBar(
                tabs = tabs,
                activeTabId = tabs.first().tabId,
                canCreateNewTab = true,
                onSelect = {},
                onClose = {},
                onCreate = {},
                onOpenSettings = {},
            )
            }
        }
        onNodeWithText("Label 1").assertExists()
        onNodeWithText("Label 2").assertExists()
        onNodeWithText("Label 3").assertExists()
    }

    @Test
    fun newTabButtonDisabledAtLimit() = runComposeUiTest {
        val tabs = makeTabs(MAX_OPEN_TABS)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
            TabBar(
                tabs = tabs,
                activeTabId = tabs.first().tabId,
                canCreateNewTab = false,
                onSelect = {},
                onClose = {},
                onCreate = {},
                onOpenSettings = {},
            )
            }
        }
        onNodeWithTag(TestTags.TAB_NEW_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun newTabButtonEnabledBelowLimit() = runComposeUiTest {
        val tabs = makeTabs(1)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
            TabBar(
                tabs = tabs,
                activeTabId = tabs.first().tabId,
                canCreateNewTab = true,
                onSelect = {},
                onClose = {},
                onCreate = {},
                onOpenSettings = {},
            )
            }
        }
        onNodeWithTag(TestTags.TAB_NEW_BUTTON).assertIsEnabled()
    }

    @Test
    fun clickingNewTabButtonTriggersOnCreate() = runComposeUiTest {
        var createCount = 0
        val tabs = makeTabs(1)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
            TabBar(
                tabs = tabs,
                activeTabId = tabs.first().tabId,
                canCreateNewTab = true,
                onSelect = {},
                onClose = {},
                onCreate = { createCount += 1 },
                onOpenSettings = {},
            )
            }
        }
        onNodeWithTag(TestTags.TAB_NEW_BUTTON).performClick()
        assertEquals(1, createCount)
    }

    @Test
    fun settingsGearIsAccessible() = runComposeUiTest {
        var settingsCount = 0
        val tabs = makeTabs(1)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
            TabBar(
                tabs = tabs,
                activeTabId = tabs.first().tabId,
                canCreateNewTab = true,
                onSelect = {},
                onClose = {},
                onCreate = {},
                onOpenSettings = { settingsCount += 1 },
            )
            }
        }
        onNodeWithTag(TestTags.SETTINGS_GEAR_ICON).performClick()
        assertEquals(1, settingsCount)
    }
}
