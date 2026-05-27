package com.strangeparticle.springboard.app.unit.ui.luther

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.luther.AiChatPaneDefaults
import com.strangeparticle.springboard.app.ui.luther.ChatPaneResizeHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class ChatPaneResizeHandleTest {

    @Test
    fun `dragging handle upward grows the chat pane height`() = runComposeUiTest {
        val tracker = HeightTracker(startDp = AiChatPaneDefaults.DefaultHeight.value)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ResizableHarness(tracker = tracker)
            }
        }

        // Drag up = negative Y. Pane grows because its bottom edge is pinned.
        onNodeWithTag(TestTags.AI_CHAT_RESIZE_HANDLE).performTouchInput {
            down(center)
            moveBy(Offset(0f, -60f))
            up()
        }

        assertTrue(
            tracker.currentHeightDp > AiChatPaneDefaults.DefaultHeight.value,
            "Dragging up should increase height; was ${AiChatPaneDefaults.DefaultHeight.value}, now ${tracker.currentHeightDp}",
        )
    }

    @Test
    fun `dragging handle downward shrinks the chat pane height`() = runComposeUiTest {
        val tracker = HeightTracker(startDp = AiChatPaneDefaults.DefaultHeight.value)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ResizableHarness(tracker = tracker)
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_RESIZE_HANDLE).performTouchInput {
            down(center)
            moveBy(Offset(0f, 60f))
            up()
        }

        assertTrue(
            tracker.currentHeightDp < AiChatPaneDefaults.DefaultHeight.value,
            "Dragging down should decrease height; was ${AiChatPaneDefaults.DefaultHeight.value}, now ${tracker.currentHeightDp}",
        )
    }

    @Test
    fun `dragging handle aggressively upward clamps to max height`() = runComposeUiTest {
        val tracker = HeightTracker(startDp = AiChatPaneDefaults.DefaultHeight.value)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ResizableHarness(tracker = tracker)
            }
        }

        // Far more upward distance than (MaxHeight - DefaultHeight) ⇒ should clamp.
        onNodeWithTag(TestTags.AI_CHAT_RESIZE_HANDLE).performTouchInput {
            down(center)
            moveBy(Offset(0f, -5000f))
            up()
        }

        assertEquals(AiChatPaneDefaults.MaxHeight.value, tracker.currentHeightDp)
    }

    @Test
    fun `dragging handle aggressively downward clamps to min height`() = runComposeUiTest {
        val tracker = HeightTracker(startDp = AiChatPaneDefaults.DefaultHeight.value)
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                ResizableHarness(tracker = tracker)
            }
        }

        onNodeWithTag(TestTags.AI_CHAT_RESIZE_HANDLE).performTouchInput {
            down(center)
            moveBy(Offset(0f, 5000f))
            up()
        }

        assertEquals(AiChatPaneDefaults.MinHeight.value, tracker.currentHeightDp)
    }

    private class HeightTracker(startDp: Float) {
        var currentHeightDp: Float = startDp
    }

    /**
     * Renders just the resize handle wired to the same clamping/density logic
     * `MainScreen` uses, so the test exercises the production drag math without
     * pulling in the whole app.
     */
    @Composable
    private fun ResizableHarness(tracker: HeightTracker) {
        var heightDp by remember { mutableStateOf(tracker.currentHeightDp) }
        val density = LocalDensity.current
        Column(modifier = Modifier.fillMaxWidth()) {
            ChatPaneResizeHandle(
                onDragDelta = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp() }
                    val proposed = (heightDp.dp - deltaDp).coerceIn(
                        AiChatPaneDefaults.MinHeight,
                        AiChatPaneDefaults.MaxHeight,
                    )
                    heightDp = proposed.value
                    tracker.currentHeightDp = heightDp
                },
            )
        }
    }
}
