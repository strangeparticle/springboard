package com.strangeparticle.springboard.app.unit.ui.editio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.editio.AiChatPane
import com.strangeparticle.springboard.app.ui.editio.AiChatPaneState
import com.strangeparticle.springboard.app.ui.editio.ChatMessagePartRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class AiChatPaneTest {

    @Test
    fun `renders not configured state with settings action`() = runComposeUiTest {
        var settingsClickCount = 0
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = AiChatPaneState.notConfigured(),
                    onClose = {},
                    onOpenSettings = { settingsClickCount += 1 },
                )
            }
        }

        onNodeWithText("AI is not configured").assertExists()
        onNodeWithTag(TestTags.AI_CHAT_SETTINGS_BUTTON).performClick()

        assertEquals(1, settingsClickCount)
    }

    @Test
    fun `renders transcript parts and approval callbacks`() = runComposeUiTest {
        val decisions = mutableListOf<Boolean>()
        val toolCall = ToolCall("call-save", "save_springboard", "{}")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ChatMessagePartRenderer(
                    part = ChatMessagePart.ToolCall(toolCall, ToolCallState.ApprovalRequested),
                    onApprovalDecision = { _, approved -> decisions += approved },
                )
            }
        }

        onNodeWithText("Tool: save_springboard").assertExists()
        onNodeWithTag(TestTags.AI_APPROVAL_APPLY_BUTTON).performClick()

        assertEquals(listOf(true), decisions)
    }

    @Test
    fun `input row sends text through pane state`() = runComposeUiTest {
        val sentMessages = mutableListOf<String>()
        val state = AiChatPaneState.configured(
            providerLabel = "OpenAI",
            modelLabel = "gpt-5",
            transcriptParts = emptyList(),
            onSubmit = { sentMessages += it },
            onStop = {},
            onApprovalDecision = { _, _ -> },
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("Add Chrome")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        assertEquals(listOf("Add Chrome"), sentMessages)
    }
}
