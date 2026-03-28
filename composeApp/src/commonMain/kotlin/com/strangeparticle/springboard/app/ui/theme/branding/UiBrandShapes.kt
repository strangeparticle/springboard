package com.strangeparticle.springboard.app.ui.theme.branding

import androidx.compose.foundation.shape.CornerBasedShape

/**
 * Shape overrides for a UI brand layer. Each slot corresponds to one
 * of the 4 Material 3 shape size categories. All properties are nullable
 * so partial overrides are supported.
 */
data class UiBrandShapes(
    val small: CornerBasedShape? = null,
    val medium: CornerBasedShape? = null,
    val large: CornerBasedShape? = null,
    val extraLarge: CornerBasedShape? = null,
)
