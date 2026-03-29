package com.strangeparticle.springboard.app.ui.theme.branding

import androidx.compose.ui.graphics.Color

/**
 * Color overrides for a UI brand layer. Every property is nullable so that
 * partial overrides are supported — only the colors a brand layer cares about
 * need to be specified. Unset colors will fall through to the next layer
 * during brand merging.
 */
data class UiBrandColors(
    // Navbar
    val navbarBackground: Color? = null,
    val navbarText: Color? = null,
    val accentMagenta: Color? = null,

    // Grid cell and row highlights
    val cellHighlight: Color? = null,
    val rowHighlight: Color? = null,
    val columnHighlight: Color? = null,
    val headerHoverBackground: Color? = null,
    val emptyCellBackground: Color? = null,
    val activeCellIndicator: Color? = null,

    // Toast — error
    val toastErrorBackground: Color? = null,
    val toastErrorBorder: Color? = null,
    val toastErrorText: Color? = null,

    // Toast — warning
    val toastWarningBackground: Color? = null,
    val toastWarningBorder: Color? = null,
    val toastWarningText: Color? = null,

    // Toast — info
    val toastInfoBackground: Color? = null,
    val toastInfoBorder: Color? = null,
    val toastInfoText: Color? = null,

    // Status bar
    val statusBarBackground: Color? = null,
    val statusBarText: Color? = null,

    // Grid structure
    val gridBorderColor: Color? = null,
    val gridDividerColor: Color? = null,

    // Guidance panel
    val guidanceBackground: Color? = null,
    val guidanceHeaderBackground: Color? = null,
    val guidanceBorder: Color? = null,
    val guidanceHeadingText: Color? = null,
    val guidanceBodyText: Color? = null,
    val guidanceCopyIcon: Color? = null,

    // Settings — header and layout
    val settingsHeaderText: Color? = null,
    val settingsColumnBackground: Color? = null,
    val settingsColumnHeaderText: Color? = null,
    val settingsColumnSubheaderText: Color? = null,
    val settingsKeyText: Color? = null,
    val settingsValueText: Color? = null,
    val settingsTooltipValueText: Color? = null,
    val settingsTooltipUnderline: Color? = null,
    val settingsDescriptionText: Color? = null,
    val settingsCardBackground: Color? = null,
    val settingsNoValueText: Color? = null,
    val settingsDivider: Color? = null,
    val settingsSubDivider: Color? = null,

    // Settings — source indicator colors
    val settingsSourceAppDefault: Color? = null,
    val settingsSourceUserSettings: Color? = null,
    val settingsSourceEnvironmentVariable: Color? = null,
    val settingsSourceCommandLine: Color? = null,

    // Settings — links
    val settingsLinkBase: Color? = null,
    val settingsLinkHover: Color? = null,
    val settingsLinkPressed: Color? = null,
    val settingsLinkBackgroundDefault: Color? = null,
    val settingsLinkBackgroundHover: Color? = null,
    val settingsLinkBackgroundPressed: Color? = null,

    // Primary action button
    val primaryActionButton: Color? = null,

    // Key navigation focus indicator
    val keyNavFocusIndicator: Color? = null,
    val keyNavFocusIndicatorUnfocused: Color? = null,

    // Activator preview
    val activatorPreviewText: Color? = null,
)
