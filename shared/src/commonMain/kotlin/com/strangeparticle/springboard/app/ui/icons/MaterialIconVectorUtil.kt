package com.strangeparticle.springboard.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Builds a 24x24 [ImageVector] from a Material icon SVG path string (the `d` attribute
 * taken verbatim from the official Material Icons catalog).
 *
 * The vector is filled with solid black as a placeholder; call sites render these through
 * Compose's `Icon`, which applies the real tint. These hand-bundled vectors replace the
 * deprecated `material-icons-core`/`material-icons-extended` artifacts so the app ships only
 * the handful of icons it actually uses instead of the full ~7,000-icon catalog.
 *
 * @param autoMirror mirror the vector under right-to-left layout (used by directional icons
 *   such as the back arrow).
 */
fun materialIconVector(
    name: String,
    pathData: String,
    autoMirror: Boolean = false,
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}
