package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.ui.graphics.Color
import com.strangeparticle.springboard.app.ui.brand.infrastructure.CustomColors

/**
 * Default custom color values for the StrangeParticle light theme.
 */
val StrangeParticleCustomColorsLightTheme = CustomColors(
    // Navbar
    navbarBackground = Color(0xFF1E1E2E),
    navbarText = Color(0xFFCDD6F4),

    // Toast — warning
    toastWarningBackground = Color(0xFFFFF8E1),
    toastWarningBorder = Color(0xFFFFE082),
    toastWarningText = Color(0xFF5D4037),

    // Toast — info
    toastInfoBackground = Color(0xFFE8F5E9),
    toastInfoBorder = Color(0xFFA5D6A7),
    toastInfoText = Color(0xFF2E7D32),

    // Guidance panel
    guidanceBackground = Color(0xFFFFFCF4),
    guidanceHeaderBackground = Color(0xFFF5EFE0),
    guidanceBorder = Color(0xFFDCCFB0),
    guidanceIndicator = Color(0xFFCCCCCC),
    guidanceHeadingText = Color(0xFF5F4B32),
    guidanceBodyText = Color(0xFF2E2A25),
    guidanceCopyIcon = Color(0xFF8A7D6B),

    // Settings — text hierarchy
    settingsValueText = Color(0xFF555555),
    settingsTooltipValueText = Color(0xFF4A4A4A),
    settingsTooltipUnderline = Color(0xFF5F5F5F),
    settingsDescriptionText = Color(0xFF888888),
    settingsNoValueText = Color(0xFF999999),

    // Settings — source indicator colors
    settingsSourceAppDefault = Color(0xFF999999),
    settingsSourceEnvironmentVariable = Color(0xFFE65100),
    settingsSourceCommandLine = Color(0xFF6A1B9A),

    // Settings — links
    settingsLinkBase = Color(0xFFAA6600),
    settingsLinkHover = Color(0xFFCC8800),
    settingsLinkPressed = Color(0xFF884400),
    settingsLinkBackgroundDefault = Color(0x10AA6600),
    settingsLinkBackgroundHover = Color(0x18AA6600),
    settingsLinkBackgroundPressed = Color(0x28AA6600),

    // Key navigation focus indicator
    keyNavFocusIndicator = Color(0xFF1E6FFF),
    keyNavFocusIndicatorUnfocused = Color.White.copy(alpha = 0.3f),

    // Activator preview
    activatorPreviewText = Color(0xFF888888),
)
