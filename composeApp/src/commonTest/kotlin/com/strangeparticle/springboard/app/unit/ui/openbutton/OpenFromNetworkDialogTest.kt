package com.strangeparticle.springboard.app.unit.ui.openbutton

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.ui.openbutton.OpenFromNetworkDialog
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OpenFromNetworkDialogTest {

    @Test
    fun `pressing enter confirms dialog`() = runComposeUiTest {
        var confirmedUrl: String? = null

        setContent {
            OpenFromNetworkDialog(
                onConfirm = { confirmedUrl = it },
                onDismiss = {},
            )
        }

        val inputField = onNode(hasSetTextAction())
        inputField.assertExists().assertIsFocused()
        inputField.performTextInput("  https://example.com/config.json  ")
        inputField.performKeyInput { pressKey(Key.Enter) }

        waitForIdle()
        assertEquals("https://example.com/config.json", confirmedUrl)
    }

    @Test
    fun `pressing escape dismisses dialog`() = runComposeUiTest {
        var dismissCount = 0

        setContent {
            OpenFromNetworkDialog(
                onConfirm = {},
                onDismiss = { dismissCount += 1 },
            )
        }

        val inputField = onNode(hasSetTextAction())
        inputField.assertExists()
        inputField.performClick()
        inputField.performKeyInput { pressKey(Key.Escape) }

        waitForIdle()
        assertEquals(1, dismissCount)
    }

    @Test
    fun `url field is auto-focused when dialog opens`() = runComposeUiTest {
        setContent {
            OpenFromNetworkDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }

        onNode(hasSetTextAction())
            .assertExists()
            .assertIsFocused()
    }
}
