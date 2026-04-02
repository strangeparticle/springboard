package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.ui.graphics.Color
import com.strangeparticle.springboard.app.ui.brand.infrastructure.CustomColors

/**
 * Default custom color values for the StrangeParticle dark theme.
 */
val StrangeParticleCustomColorsDarkTheme = CustomColors(
    // Navbar
    navbarBackground = Color(0xFF0D0D12),
    navbarText = Color(0xFFE6E6F2),

    // Toast — warning
    toastWarningBackground = Color(0xFF3A2F16),
    toastWarningBorder = Color(0xFF8A6E2F),
    toastWarningText = Color(0xFFFFE7B3),

    // Toast — info
    toastInfoBackground = Color(0xFF183328),
    toastInfoBorder = Color(0xFF2E7D5A),
    toastInfoText = Color(0xFFBFEBD5),

    // Guidance panel
    guidanceBackground = Color(0xFF1D1A14),
    guidanceHeaderBackground = Color(0xFF2A241B),
    guidanceBorder = Color(0xFF6D5F43),
    guidanceIndicator = Color(0xFFA4A4A4),
    guidanceHeadingText = Color(0xFFE6D4B0),
    guidanceBodyText = Color(0xFFE3DED4),
    guidanceCopyIcon = Color(0xFFC9B89A),

    // Settings — text hierarchy
    settingsValueText = Color(0xFFD0D0D0),
    settingsTooltipValueText = Color(0xFFDADADA),
    settingsTooltipUnderline = Color(0xFFB5B5B5),
    settingsDescriptionText = Color(0xFFAFAFAF),
    settingsNoValueText = Color(0xFF9D9D9D),

    // Settings — source indicator colors
    settingsSourceAppDefault = Color(0xFF9D9D9D),
    settingsSourceEnvironmentVariable = Color(0xFFFFB366),
    settingsSourceCommandLine = Color(0xFFD8A6FF),

    // Settings — links
    settingsLinkBase = Color(0xFFE0A84A),
    settingsLinkHover = Color(0xFFF0BC62),
    settingsLinkPressed = Color(0xFFC38A2B),
    settingsLinkBackgroundDefault = Color(0x22E0A84A),
    settingsLinkBackgroundHover = Color(0x2AE0A84A),
    settingsLinkBackgroundPressed = Color(0x3AE0A84A),

    // Key navigation focus indicator
    keyNavFocusIndicator = Color(0xFF7FB0FF),
    keyNavFocusIndicatorUnfocused = Color.White.copy(alpha = 0.4f),

    // Activator preview
    activatorPreviewText = Color(0xFFB8B8B8),
)
