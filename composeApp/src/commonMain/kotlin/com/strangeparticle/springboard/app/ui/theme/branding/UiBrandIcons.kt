package com.strangeparticle.springboard.app.ui.theme.branding

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single icon reference that can be either a Material icon
 * (ImageVector) or a painted resource (Painter). This sealed type allows
 * the brand system to handle both kinds of icon uniformly.
 */
sealed class UiBrandIconReference {
    data class Vector(val imageVector: ImageVector) : UiBrandIconReference()
    data class Painted(val painter: Painter) : UiBrandIconReference()
}

/**
 * Icon overrides for a UI brand layer. Maps icon keys to icon references.
 * Only the icons a brand layer cares about need to be included in the map.
 */
data class UiBrandIcons(
    val icons: Map<UiBrandIconKey, UiBrandIconReference> = emptyMap(),
)
