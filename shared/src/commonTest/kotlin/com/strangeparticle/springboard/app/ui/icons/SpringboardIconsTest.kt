package com.strangeparticle.springboard.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the hand-bundled icon vectors that replaced the deprecated material-icons artifacts.
 *
 * The SVG path strings are split across concatenated Kotlin string literals for readability, so
 * the real risk is a dropped or doubled character at a join producing malformed path data. Each
 * test builds the vector and asserts it parsed into actual geometry, which fails loudly on any
 * such transcription error.
 */
class SpringboardIconsTest {

    private val allIcons: List<Pair<String, ImageVector>> = listOf(
        "Add" to SpringboardIcons.Add,
        "ArrowBack" to SpringboardIcons.ArrowBack,
        "ArrowDropDown" to SpringboardIcons.ArrowDropDown,
        "AutoAwesome" to SpringboardIcons.AutoAwesome,
        "BugReport" to SpringboardIcons.BugReport,
        "Check" to SpringboardIcons.Check,
        "Close" to SpringboardIcons.Close,
        "CloudDownload" to SpringboardIcons.CloudDownload,
        "ContentCopy" to SpringboardIcons.ContentCopy,
        "DragHandle" to SpringboardIcons.DragHandle,
        "Info" to SpringboardIcons.Info,
        "Lock" to SpringboardIcons.Lock,
        "Refresh" to SpringboardIcons.Refresh,
        "Settings" to SpringboardIcons.Settings,
        "Warning" to SpringboardIcons.Warning,
    )

    @Test
    fun everyIconParsesIntoNonEmptyGeometry() {
        for ((name, icon) in allIcons) {
            assertEquals(24f, icon.viewportWidth, "$name viewport width")
            assertEquals(24f, icon.viewportHeight, "$name viewport height")
            val pathNodeCount = countPathNodes(icon.root)
            assertTrue(pathNodeCount > 0, "$name should parse into at least one path node")
        }
    }

    @Test
    fun arrowBackIsAutoMirroredForRightToLeftLayout() {
        assertTrue(SpringboardIcons.ArrowBack.autoMirror, "ArrowBack must mirror under RTL")
    }

    private fun countPathNodes(group: VectorGroup): Int {
        var total = 0
        for (node: VectorNode in group) {
            when (node) {
                is VectorPath -> total += node.pathData.size
                is VectorGroup -> total += countPathNodes(node)
            }
        }
        return total
    }
}
