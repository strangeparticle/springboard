package com.strangeparticle.springboard.app.ui.theme.branding

import androidx.compose.ui.text.TextStyle

/**
 * Typography overrides for a UI brand layer. Each slot corresponds to one
 * of the 15 Material 3 typography roles. All properties are nullable so
 * partial overrides are supported.
 */
data class UiBrandTypography(
    val displayLarge: TextStyle? = null,
    val displayMedium: TextStyle? = null,
    val displaySmall: TextStyle? = null,
    val headlineLarge: TextStyle? = null,
    val headlineMedium: TextStyle? = null,
    val headlineSmall: TextStyle? = null,
    val titleLarge: TextStyle? = null,
    val titleMedium: TextStyle? = null,
    val titleSmall: TextStyle? = null,
    val bodyLarge: TextStyle? = null,
    val bodyMedium: TextStyle? = null,
    val bodySmall: TextStyle? = null,
    val labelLarge: TextStyle? = null,
    val labelMedium: TextStyle? = null,
    val labelSmall: TextStyle? = null,
)
