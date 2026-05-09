package com.strangeparticle.springboard.app.unit.ui.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.tabs.TabIndicator
import com.strangeparticle.springboard.app.ui.tabs.TabStatusIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TabIndicatorTest {

    @Test
    fun rendersLabel() = runComposeUiTest {
        setContent {
            TabIndicator(
                label = "my-config",
                isActive = true,
                statusIcon = null,
                tabId = "tab-1",
                onSelect = {},
                onClose = {},
            )
        }
        onNodeWithText("my-config").assertExists()
    }

    @Test
    fun closeIconIsAccessibleByLabel() = runComposeUiTest {
        setContent {
            TabIndicator(
                label = "tab-x",
                isActive = false,
                statusIcon = null,
                tabId = "tab-1",
                onSelect = {},
                onClose = {},
            )
        }
        onNode(hasContentDescription("Close tab tab-x")).assertExists().assertHasClickAction()
    }

    @Test
    fun clickingBodyTriggersOnSelect() = runComposeUiTest {
        var selectCount = 0
        setContent {
            TabIndicator(
                label = "my-tab",
                isActive = false,
                statusIcon = null,
                tabId = "tab-1",
                onSelect = { selectCount += 1 },
                onClose = {},
            )
        }
        onNodeWithText("my-tab").performClick()
        assertEquals(1, selectCount)
    }

    @Test
    fun clickingCloseIconTriggersOnClose() = runComposeUiTest {
        var closeCount = 0
        setContent {
            TabIndicator(
                label = "my-tab",
                isActive = true,
                statusIcon = null,
                tabId = "tab-1",
                onSelect = {},
                onClose = { closeCount += 1 },
            )
        }
        onNode(hasContentDescription("Close tab my-tab")).performClick()
        assertEquals(1, closeCount)
    }

    @Test
    fun rendersDirtyIconWhenStatusIsDirty() = runComposeUiTest {
        setContent {
            TabIndicator(
                label = "my-tab",
                isActive = false,
                statusIcon = TabStatusIcon.Dirty,
                tabId = "tab-7",
                onSelect = {},
                onClose = {},
            )
        }
        onNodeWithTag(TestTags.tabDirtyIndicator("tab-7"), useUnmergedTree = true).assertExists()
        // The lock indicator is mutually exclusive — should not be present.
        onNodeWithTag(TestTags.tabLockIndicator("tab-7")).assertDoesNotExist()
    }

    @Test
    fun rendersNonSaveableIconWhenStatusIsNonSaveable() = runComposeUiTest {
        setContent {
            TabIndicator(
                label = "my-tab",
                isActive = false,
                statusIcon = TabStatusIcon.NonSaveable,
                tabId = "tab-9",
                onSelect = {},
                onClose = {},
            )
        }
        onNodeWithTag(TestTags.tabLockIndicator("tab-9"), useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.tabDirtyIndicator("tab-9")).assertDoesNotExist()
    }

    @Test
    fun rendersNeitherIndicatorWhenStatusIsNull() = runComposeUiTest {
        setContent {
            TabIndicator(
                label = "my-tab",
                isActive = false,
                statusIcon = null,
                tabId = "tab-3",
                onSelect = {},
                onClose = {},
            )
        }
        onNodeWithTag(TestTags.tabDirtyIndicator("tab-3")).assertDoesNotExist()
        onNodeWithTag(TestTags.tabLockIndicator("tab-3")).assertDoesNotExist()
    }
}
