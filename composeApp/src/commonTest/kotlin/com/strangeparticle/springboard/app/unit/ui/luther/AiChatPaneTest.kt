package com.strangeparticle.springboard.app.unit.ui.luther

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.luther.session.ChatMessagePart
import com.strangeparticle.luther.session.ToolCallState
import com.strangeparticle.luther.toolcall.ToolCall
import com.strangeparticle.springboard.app.luther.help.AiAssistantFullHelpText
import com.strangeparticle.springboard.app.luther.help.AiAssistantTerseHelpText
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.luther.AiChatPane
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneDefaults
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneState
import com.strangeparticle.springboard.app.ui.luther.AiChatScrollbackPane
import com.strangeparticle.springboard.app.ui.luther.CommandAttribution
import com.strangeparticle.springboard.app.ui.luther.ChatMessagePartRenderer
import com.strangeparticle.springboard.app.ui.luther.LocalCommandResponseStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

        onNodeWithText("Approval requested").assertExists()
        onNodeWithTag(TestTags.AI_APPROVAL_APPLY_BUTTON).performClick()

        assertEquals(listOf(true), decisions)
    }

    @Test
    fun `successful tool calls are not shown in chat transcript`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ChatMessagePartRenderer(
                    part = ChatMessagePart.ToolCall(
                        ToolCall("call-group", "add_app_group", "{}"),
                        ToolCallState.OutputAvailable("Applied."),
                    ),
                    onApprovalDecision = { _, _ -> },
                )
            }
        }

        onNodeWithText("Tool: add_app_group").assertDoesNotExist()
        onNodeWithText("Applied.").assertDoesNotExist()
    }

    @Test
    fun `provider error after tool call does not show tool success`() = runComposeUiTest {
        val state = configuredState(
            transcriptParts = listOf(
                ChatMessagePart.UserText("Group these apps"),
                ChatMessagePart.ToolCall(
                    ToolCall("call-group", "add_app_group", "{}"),
                    ToolCallState.OutputAvailable("Applied."),
                ),
                ChatMessagePart.ChatError("OpenAI request failed with HTTP 429: Request too large"),
            ),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithText("Group these apps").assertExists()
        onNodeWithText("Error: OpenAI request failed with HTTP 429: Request too large").assertExists()
        onNodeWithText("Tool: add_app_group").assertDoesNotExist()
        onNodeWithText("Applied.").assertDoesNotExist()
    }

    @Test
    fun `interaction messages expose distinct visual roles`() = runComposeUiTest {
        val state = configuredState(
            transcriptParts = listOf(
                ChatMessagePart.UserText("Add Chrome"),
                ChatMessagePart.AssistantText("I can do that."),
                ChatMessagePart.ToolCall(
                    ToolCall("call-save", "save_springboard", "{}"),
                    ToolCallState.ApprovalRequested,
                ),
                ChatMessagePart.ChatError("network unavailable"),
            ),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {}, height = 420.dp)
            }
        }

        val pane = onNodeWithTag(TestTags.aiChatScrollbackPane(0)).getUnclippedBoundsInRoot()
        val user = onNodeWithTag(TestTags.AI_CHAT_USER_MESSAGE, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val assistant = onNodeWithTag(TestTags.AI_CHAT_ASSISTANT_MESSAGE, useUnmergedTree = true).getUnclippedBoundsInRoot()

        val error = onNodeWithTag(TestTags.AI_CHAT_ERROR_MESSAGE, useUnmergedTree = true).getUnclippedBoundsInRoot()

        onNodeWithTag(TestTags.AI_CHAT_TOOL_ACTIVITY, useUnmergedTree = true).assertExists()
        onNodeWithText("You").assertDoesNotExist()
        onNodeWithText("Assistant").assertDoesNotExist()
        onNodeWithText("Tool").assertDoesNotExist()
        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithText("You: Add Chrome").assertDoesNotExist()
        onNodeWithText("Add Chrome").assertExists()
        onNodeWithText("Error: network unavailable").assertExists()

        assertTrue(user.left < pane.left + 72.dp, "user message should sit on the left side of its pane")
        assertTrue(assistant.left < pane.left + 72.dp, "assistant message should sit on the left side of its pane")
        assertTrue(error.top > user.bottom + 8.dp, "response/error should have breathing room below the user message")
    }

    @Test
    fun `user message bubble uses filled button primary color`() = runComposeUiTest {
        var expectedBubbleColor = Color.Unspecified

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                expectedBubbleColor = MaterialTheme.colorScheme.primary
                ChatMessagePartRenderer(
                    part = ChatMessagePart.UserText("      "),
                    onApprovalDecision = { _, _ -> },
                )
            }
        }

        val pixels = onNodeWithTag(TestTags.AI_CHAT_USER_MESSAGE, useUnmergedTree = true).captureToImage().toPixelMap()

        assertEquals(expectedBubbleColor, pixels[pixels.width / 2, pixels.height / 2])
    }

    @Test
    fun `local commands expose command visual role`() = runComposeUiTest {
        val state = configuredState(
            scrollbackPanes = listOf(
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help",
                    commandAttribution = CommandAttribution.User,
                    responseText = AiAssistantFullHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
            ),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_COMMAND_MESSAGE, useUnmergedTree = true).assertExists()
        onNodeWithText("Command").assertDoesNotExist()
        onNodeWithText("You: /help").assertExists()
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

    @Test
    fun `enter sends while shift enter inserts a newline`() = runComposeUiTest {
        val sentMessages = mutableListOf<String>()
        val state = configuredState(onSubmit = { sentMessages += it })

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        val input = onNodeWithTag(TestTags.AI_CHAT_INPUT)
        input.performTextInput("line one")
        input.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Enter)
            keyUp(Key.ShiftLeft)
        }
        input.performTextInput("line two")
        input.assertTextEquals("line one\nline two")
        assertEquals(emptyList(), sentMessages)

        input.performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf("line one\nline two"), sentMessages)
    }

    @Test
    fun `pasting tabs preserves tab characters`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        val input = onNodeWithTag(TestTags.AI_CHAT_INPUT)
        input.performTextInput("before\tafter")

        input.assertTextEquals("before\tafter")
    }

    @Test
    fun `stop button stops when running`() = runComposeUiTest {
        var stopCount = 0
        val state = configuredState(isRunning = true, onStop = { stopCount += 1 })

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_STOP_BUTTON).performClick()

        assertEquals(1, stopCount)
    }

    @Test
    fun `running state disables input editing and submit`() = runComposeUiTest {
        val sentMessages = mutableListOf<String>()
        val state = configuredState(
            isRunning = true,
            onSubmit = { sentMessages += it },
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).assertIsNotEnabled()
        onNodeWithTag(TestTags.AI_CHAT_STOP_BUTTON).assertIsEnabled()
        onNodeWithTag(TestTags.AI_CHAT_WORKING_INDICATOR).assertExists()

        val input = onNodeWithTag(TestTags.AI_CHAT_INPUT)
        input.assertIsNotEnabled()
        input.assertTextEquals("")
        assertEquals(emptyList(), sentMessages)
    }

    @Test
    fun `idle state hides processing indicator and allows submit`() = runComposeUiTest {
        val sentMessages = mutableListOf<String>()
        val state = configuredState(
            isRunning = false,
            onSubmit = { sentMessages += it },
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_WORKING_INDICATOR).assertDoesNotExist()
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).assertIsEnabled()
        onNodeWithTag(TestTags.AI_CHAT_STOP_BUTTON).assertIsNotEnabled()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).performTextInput("Add Chrome")
        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).performClick()

        assertEquals(listOf("Add Chrome"), sentMessages)
    }

    @Test
    fun `running transition requests processing focus fallback`() = runComposeUiTest {
        var fallbackCount = 0
        val isRunning = mutableStateOf(false)

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(
                        isRunning = isRunning.value,
                        onProcessingFocusFallback = { fallbackCount += 1 },
                    ),
                    onClose = {},
                    onOpenSettings = {},
                )
            }
        }

        waitForIdle()
        assertEquals(0, fallbackCount)

        isRunning.value = true
        waitForIdle()

        assertEquals(1, fallbackCount)
    }

    @Test
    fun `focus returns to input after running completes`() = runComposeUiTest {
        val isRunning = mutableStateOf(true)

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(isRunning = isRunning.value),
                    onClose = {},
                    onOpenSettings = {},
                )
            }
        }

        waitForIdle()
        isRunning.value = false
        waitForIdle()

        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertIsFocused()
    }

    @Test
    fun `chat pane defaults to default height and fixed three line input`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_PANE).assertHeightIsEqualTo(270.dp)
        onNodeWithTag(TestTags.AI_CHAT_HISTORY).assertExists()
        onNodeWithTag(TestTags.AI_CHAT_INPUT).assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun `chat pane renders at the explicit height passed in`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(),
                    onClose = {},
                    onOpenSettings = {},
                    height = 360.dp,
                )
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_PANE).assertHeightIsEqualTo(360.dp)
    }

    @Test
    fun `input field left edge aligns with scrollback panes`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        val scrollbackLeft = onNodeWithTag(TestTags.aiChatScrollbackPane(0)).getUnclippedBoundsInRoot().left
        val inputLeft = onNodeWithTag(TestTags.AI_CHAT_INPUT_SECTION).getUnclippedBoundsInRoot().left

        assertEquals(scrollbackLeft.value, inputLeft.value, absoluteTolerance = 0.5f)
    }

    @Test
    fun `input section uses lighter background than pane body`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                val colors = AiChatPaneDefaults.colors()
                assertEquals(MaterialTheme.colorScheme.surfaceContainerLow, colors.pane)
                assertEquals(MaterialTheme.colorScheme.surfaceContainer, colors.inputSection)
                assertNotEquals(colors.pane, colors.inputSection)
            }
        }
    }

    @Test
    fun `scrollback panes use distinct surfaces and outline`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                val colors = AiChatPaneDefaults.colors()
                assertEquals(MaterialTheme.colorScheme.surfaceContainerLow, colors.pane)
                assertEquals(MaterialTheme.colorScheme.surfaceContainerHigh, colors.interactionScrollbackPane)
                assertEquals(MaterialTheme.colorScheme.surfaceContainer, colors.helpScrollbackPane)
                assertEquals(MaterialTheme.colorScheme.outlineVariant, colors.scrollbackPaneOutline)
                assertNotEquals(colors.pane, colors.interactionScrollbackPane)
                assertNotEquals(colors.pane, colors.helpScrollbackPane)
            }
        }
    }

    @Test
    fun `scrollback text uses compact line height`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                val textStyle = AiChatPaneDefaults.scrollbackTextStyle()
                assertEquals(13.sp, textStyle.fontSize)
                assertEquals(17.sp, textStyle.lineHeight)
            }
        }
    }

    @Test
    fun `startup terse help renders as a scrollback pane with command text`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.aiChatScrollbackPane(0)).assertExists()
        onNodeWithText("/help_terse").assertExists()
        onNodeWithText("You: /help_terse").assertDoesNotExist()
        onNodeWithText(AiAssistantTerseHelpText.text.lines().first(), substring = true).assertExists()
        onNodeWithTag(TestTags.aiChatScrollbackPaneCopyButton(0)).assertExists()
    }

    @Test
    fun `chat header uses AI assistant title and secondary provider text`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithText("AI Assistant").assertExists()
        onNodeWithText("OpenAI: gpt-5").assertExists()
        onNodeWithText("OpenAI · gpt-5").assertDoesNotExist()
        onNodeWithText("Assistant · OpenAI gpt-5").assertDoesNotExist()
    }

    @Test
    fun `send and stop buttons are compact`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_SEND_BUTTON).assertHeightIsEqualTo(32.dp)
        onNodeWithTag(TestTags.AI_CHAT_STOP_BUTTON).assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun `copy transcript button copies full transcript text`() = runComposeUiTest {
        var copiedText = ""
        val state = configuredState(
            transcriptParts = listOf(
                ChatMessagePart.UserText("Add Chrome"),
                ChatMessagePart.AssistantText("I can do that."),
                ChatMessagePart.ChatError("network unavailable"),
                ChatMessagePart.ToolCall(
                    ToolCall("call-save", "save_springboard", "{}"),
                    ToolCallState.OutputAvailable("saved"),
                ),
            ),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = state,
                    onClose = {},
                    onOpenSettings = {},
                    onCopyTranscript = { copiedText = it },
                )
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_COPY_TRANSCRIPT_BUTTON).performClick()

        assertEquals(
            "You: Add Chrome\n\nAssistant: I can do that.\nError: network unavailable",
            copiedText,
        )
    }

    @Test
    fun `copy debug chat history button copies debug dump text and sits left of transcript copy`() = runComposeUiTest {
        var copiedText = ""
        val state = configuredState(
            debugChatHistoryText = """{"kind":"SpringboardAiDebugDump"}""",
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = state,
                    onClose = {},
                    onOpenSettings = {},
                    onCopyTranscript = { copiedText = it },
                )
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_COPY_DEBUG_HISTORY_BUTTON).performClick()

        val debugButton = onNodeWithTag(TestTags.AI_CHAT_COPY_DEBUG_HISTORY_BUTTON).getUnclippedBoundsInRoot()
        val transcriptButton = onNodeWithTag(TestTags.AI_CHAT_COPY_TRANSCRIPT_BUTTON).getUnclippedBoundsInRoot()
        assertTrue(debugButton.right <= transcriptButton.left)
        assertEquals("""{"kind":"SpringboardAiDebugDump"}""", copiedText)
    }

    @Test
    fun `user-facing help is rendered when transcript is empty`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_USER_HELP).assertExists()
        onNodeWithText(AiAssistantTerseHelpText.text.lines().first(), substring = true).assertExists()
    }

    @Test
    fun `help command renders below empty-chat summary`() = runComposeUiTest {
        val state = configuredState(
            scrollbackPanes = listOf(
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help_terse",
                    commandAttribution = CommandAttribution.System,
                    responseText = AiAssistantTerseHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help",
                    commandAttribution = CommandAttribution.User,
                    responseText = AiAssistantFullHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
            ),
        )
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.aiChatScrollbackPane(1)).assertExists()
        onNodeWithText("You: /help").assertExists()
        onNodeWithText(AiAssistantFullHelpText.title, substring = true).assertExists()
        onNodeWithTag(TestTags.AI_CHAT_HISTORY).performScrollToIndex(0)
        onNodeWithTag(TestTags.aiChatScrollbackPane(0)).assertExists()
    }

    @Test
    fun `copy transcript includes local help command output`() = runComposeUiTest {
        var copiedText = ""
        val state = configuredState(
            scrollbackPanes = listOf(
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help_terse",
                    commandAttribution = CommandAttribution.System,
                    responseText = AiAssistantTerseHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help",
                    commandAttribution = CommandAttribution.User,
                    responseText = AiAssistantFullHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
            ),
        )
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = state,
                    onClose = {},
                    onOpenSettings = {},
                    onCopyTranscript = { copiedText = it },
                )
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_COPY_TRANSCRIPT_BUTTON).performClick()

        assertTrue(copiedText.contains("/help_terse"))
        assertTrue(copiedText.contains("You: /help"))
        assertTrue(copiedText.contains(AiAssistantFullHelpText.title))
    }

    @Test
    fun `pane copy button copies only that scrollback pane`() = runComposeUiTest {
        var copiedText = ""
        val state = configuredState(
            scrollbackPanes = listOf(
                AiChatScrollbackPane.LocalCommand(
                    commandText = "/help_terse",
                    commandAttribution = CommandAttribution.System,
                    responseText = AiAssistantTerseHelpText.text,
                    style = LocalCommandResponseStyle.Help,
                ),
                AiChatScrollbackPane.Interaction(
                    requestText = "Add Chrome",
                    responseParts = listOf(ChatMessagePart.AssistantText("Added Chrome.")),
                ),
            ),
        )
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = state,
                    onClose = {},
                    onOpenSettings = {},
                    onCopyTranscript = { copiedText = it },
                )
            }
        }

        onNodeWithTag(TestTags.aiChatScrollbackPaneCopyButton(1), useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        waitForIdle()

        assertEquals("You: Add Chrome\n\nAssistant: Added Chrome.", copiedText)
    }

    @Test
    fun `interaction pane renders user request once transcript has a user part`() = runComposeUiTest {
        val state = configuredState(
            transcriptParts = listOf(ChatMessagePart.UserText("Add a logs URL for fretnaut in prod")),
        )
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = state, onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithText("Add a logs URL for fretnaut in prod").assertExists()
    }

    @Test
    fun `close button is compact`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_CLOSE_BUTTON).assertHeightIsEqualTo(28.dp).assertWidthIsEqualTo(28.dp)
    }

    @Test
    fun `chat input border changes when focused`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(state = configuredState(), onClose = {}, onOpenSettings = {})
            }
        }

        val input = onNodeWithTag(TestTags.AI_CHAT_INPUT)
        val unfocusedPixels = input.captureToImage().toPixelMap()

        input.performClick()

        val focusedPixels = input.captureToImage().toPixelMap()
        assertNotEquals(pixelSignature(unfocusedPixels), pixelSignature(focusedPixels))
    }

    @Test
    fun `chat history scrolls to bottom when a new pane is appended`() = runComposeUiTest {
        val scrollbackPanes = mutableStateOf(numberedScrollbackPanes(12))

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(scrollbackPanes = scrollbackPanes.value),
                    onClose = {},
                    onOpenSettings = {},
                    height = 220.dp,
                )
            }
        }

        onNodeWithText("Request 11").assertIsDisplayed()

        scrollbackPanes.value = numberedScrollbackPanes(13)
        waitForIdle()

        onNodeWithText("Request 12").assertIsDisplayed()
    }

    @Test
    fun `chat history scrolls to bottom on initial render`() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(scrollbackPanes = numberedScrollbackPanes(12)),
                    onClose = {},
                    onOpenSettings = {},
                    height = 220.dp,
                )
            }
        }

        onNodeWithText("Request 11").assertIsDisplayed()
    }

    @Test
    fun `chat history scrolls to bottom when latest pane content grows after user scrolled up`() = runComposeUiTest {
        val initialPanes = numberedScrollbackPanes(12)
        val scrollbackPanes = mutableStateOf(initialPanes)

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AiChatPane(
                    state = configuredState(scrollbackPanes = scrollbackPanes.value),
                    onClose = {},
                    onOpenSettings = {},
                    height = 220.dp,
                )
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_HISTORY).performScrollToIndex(0)
        onNodeWithText("Request 0").assertIsDisplayed()

        scrollbackPanes.value = initialPanes.dropLast(1) + AiChatScrollbackPane.Interaction(
            requestText = "Request 11",
            responseParts = listOf(
                ChatMessagePart.AssistantText(
                    (1..20).joinToString("\n") { line -> "Expanded assistant response line $line" },
                ),
            ),
        )
        waitForIdle()

        onNodeWithText("Expanded assistant response line 20", substring = true).assertIsDisplayed()
    }

    private fun pixelSignature(pixelMap: androidx.compose.ui.graphics.PixelMap): Int {
        var signature = 0
        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                signature = 31 * signature + pixelMap[x, y].hashCode()
            }
        }
        return signature
    }

    private fun numberedScrollbackPanes(count: Int): List<AiChatScrollbackPane> = (0 until count).map { index ->
        AiChatScrollbackPane.Interaction(
            requestText = "Request $index",
            responseParts = listOf(ChatMessagePart.AssistantText("Assistant response $index")),
        )
    }

    private fun configuredState(
        isRunning: Boolean = false,
        transcriptParts: List<ChatMessagePart> = emptyList(),
        scrollbackPanes: List<AiChatScrollbackPane>? = null,
        debugChatHistoryText: String = "",
        onSubmit: (String) -> Unit = {},
        onStop: () -> Unit = {},
        onProcessingFocusFallback: () -> Unit = {},
    ): AiChatPaneState = if (scrollbackPanes == null) {
        AiChatPaneState.configured(
            providerLabel = "OpenAI",
            modelLabel = "gpt-5",
            transcriptParts = transcriptParts,
            debugChatHistoryText = debugChatHistoryText,
            isRunning = isRunning,
            onSubmit = onSubmit,
            onStop = onStop,
            onApprovalDecision = { _, _ -> },
            onProcessingFocusFallback = onProcessingFocusFallback,
        )
    } else {
        AiChatPaneState.configured(
            providerLabel = "OpenAI",
            modelLabel = "gpt-5",
            transcriptParts = transcriptParts,
            scrollbackPanes = scrollbackPanes,
            debugChatHistoryText = debugChatHistoryText,
            isRunning = isRunning,
            onSubmit = onSubmit,
            onStop = onStop,
            onApprovalDecision = { _, _ -> },
            onProcessingFocusFallback = onProcessingFocusFallback,
        )
    }
}
