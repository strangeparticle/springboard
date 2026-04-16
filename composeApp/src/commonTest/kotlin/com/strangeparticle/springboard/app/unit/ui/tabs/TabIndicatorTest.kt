package com.strangeparticle.springboard.app.unit.ui.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.ui.tabs.TabIndicator
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
                onSelect = {},
                onClose = { closeCount += 1 },
            )
        }
        onNode(hasContentDescription("Close tab my-tab")).performClick()
        assertEquals(1, closeCount)
    }
}
