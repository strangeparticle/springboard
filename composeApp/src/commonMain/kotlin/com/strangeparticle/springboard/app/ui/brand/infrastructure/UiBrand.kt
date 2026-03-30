package com.strangeparticle.springboard.app.ui.brand.infrastructure

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

/**
 * The fully resolved UI brand the app uses at runtime.
 *
 * Theme values ([colorScheme], [typography], [shapes]) are applied via
 * [MaterialTheme]. Custom colors, vector images, and drawable resources
 * are accessed directly through [customColors], [vectorImages], and
 * [drawableResources].
 */
data class UiBrand(
    val colorScheme: ColorScheme,
    val customColors: CustomColors,
    val typography: Typography,
    val shapes: Shapes,
    val vectorImages: VectorImages,
    val drawableResources: DrawableResources,
)
