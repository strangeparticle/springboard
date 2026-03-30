package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun strangeParticleColorScheme(
    primary: Color = Color(0xFF1976D2),
    onPrimary: Color = Color.White,
    primaryContainer: Color = Color(0xFF89B4FA),
    surface: Color = Color.White,
    onSurface: Color = Color(0xFF333333),
    onSurfaceVariant: Color = Color(0xFF666666),
    surfaceContainer: Color = Color(0xFFF5F5F5),
    surfaceContainerHigh: Color = Color(0xFFEEEEEE),
    surfaceContainerLowest: Color = Color(0xFFF8F8F6),
    surfaceContainerLow: Color = Color(0xFFF0F0F0),
    outline: Color = Color(0xFFE0E0E0),
    outlineVariant: Color = Color(0xFFDDDDDD),
    error: Color = Color(0xFFEF9A9A),
    errorContainer: Color = Color(0xFFFFEBEE),
    onErrorContainer: Color = Color(0xFFC62828),
): ColorScheme = lightColorScheme(
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
