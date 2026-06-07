package com.strangeparticle.springboard.app.unit.ui.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.tabs.CloseDirtyTabConfirmDialog
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [CloseDirtyTabConfirmDialog]. The dialog is dumb — it renders the label,
 * shows two buttons, and fires the right callback when each is clicked. These tests
 * verify the wiring; the surrounding lifecycle (when to show, when to dismiss) lives
 * in `MainScreen` and is exercised by acceptance tests for the close-tab flow.
 */
@OptIn(ExperimentalTestApi::class)
class CloseDirtyTabConfirmDialogTest {

    @Test
    fun rendersTitleAndTabLabel() = runComposeUiTest {
        setContent {
            CloseDirtyTabConfirmDialog(
                tabLabel = "my-config.json",
                onCancel = {},
                onCloseAnyway = {},
            )
        }
        onNodeWithText("Close without saving?").assertExists()
        onNodeWithText("my-config.json", substring = true).assertExists()
    }

    @Test
    fun closeAnywayButtonFiresOnCloseAnyway() = runComposeUiTest {
        var closeAnywayCount = 0
        var cancelCount = 0
        setContent {
            CloseDirtyTabConfirmDialog(
                tabLabel = "x",
                onCancel = { cancelCount += 1 },
                onCloseAnyway = { closeAnywayCount += 1 },
            )
        }
        onNodeWithTag(TestTags.CLOSE_DIRTY_TAB_CONFIRM_BUTTON).performClick()
        assertEquals(1, closeAnywayCount)
        assertEquals(0, cancelCount)
    }

    @Test
    fun cancelButtonFiresOnCancel() = runComposeUiTest {
        var closeAnywayCount = 0
        var cancelCount = 0
        setContent {
            CloseDirtyTabConfirmDialog(
                tabLabel = "x",
                onCancel = { cancelCount += 1 },
                onCloseAnyway = { closeAnywayCount += 1 },
            )
        }
        onNodeWithTag(TestTags.CLOSE_DIRTY_TAB_CANCEL_BUTTON).performClick()
        assertEquals(0, closeAnywayCount)
        assertEquals(1, cancelCount)
    }
}
