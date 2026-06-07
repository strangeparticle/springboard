package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

fun strangeParticleDarkColorScheme(
    primary: Color = Color(0xFF1976D2),
    onPrimary: Color = Color.White,
    primaryContainer: Color = Color(0xFF89B4FA),
    surface: Color = Color(0xFF000000),
    onSurface: Color = Color(0xFFE8E8E8),
    onSurfaceVariant: Color = Color(0xFFBBBBBB),
    surfaceContainer: Color = Color(0xFF1A1A1A),
    surfaceContainerHigh: Color = Color(0xFF252525),
    surfaceContainerLowest: Color = Color(0xFF0D0D0D),
    surfaceContainerLow: Color = Color(0xFF141414),
    outline: Color = Color(0xFF444444),
    outlineVariant: Color = Color(0xFF3A3A3A),
    error: Color = Color(0xFFEF9A9A),
    errorContainer: Color = Color(0xFFFFEBEE),
    onErrorContainer: Color = Color(0xFFC62828),
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    surface = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    outline = outline,
    outlineVariant = outlineVariant,
    error = error,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
)
