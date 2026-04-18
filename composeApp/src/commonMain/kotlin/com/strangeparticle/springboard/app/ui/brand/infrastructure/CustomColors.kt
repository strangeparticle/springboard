package com.strangeparticle.springboard.app.ui.brand.infrastructure

import androidx.compose.ui.graphics.Color

/**
 * App-specific color tokens that do not map to Material color scheme roles.
 */
data class CustomColors(
    // Navbar
    val navbarBackground: Color,
    val navbarText: Color,

    // Toast — warning
    val toastWarningBackground: Color,
    val toastWarningBorder: Color,
    val toastWarningText: Color,

    // Toast — info
    val toastInfoBackground: Color,
    val toastInfoBorder: Color,
    val toastInfoText: Color,

    // Guidance panel
    val guidanceBackground: Color,
    val guidanceHeaderBackground: Color,
    val guidanceBorder: Color,
    val guidanceIndicator: Color,
    val guidanceHeadingText: Color,
    val guidanceBodyText: Color,
    val guidanceCopyIcon: Color,

    // Settings — text hierarchy
    val settingsValueText: Color,
    val settingsTooltipValueText: Color,
    val settingsTooltipUnderline: Color,
    val settingsDescriptionText: Color,
    val settingsNoValueText: Color,

    // Settings — source indicator colors
    val settingsSourceAppDefault: Color,
    val settingsSourceEnvironmentVariable: Color,
    val settingsSourceParams: Color,

    // Settings — links
    val settingsLinkBase: Color,
    val settingsLinkHover: Color,
    val settingsLinkPressed: Color,
    val settingsLinkBackgroundDefault: Color,
    val settingsLinkBackgroundHover: Color,
    val settingsLinkBackgroundPressed: Color,

    // Key navigation focus indicator
    val keyNavFocusIndicator: Color,
    val keyNavFocusIndicatorUnfocused: Color,

    // Activator preview
    val activatorPreviewText: Color,
)
