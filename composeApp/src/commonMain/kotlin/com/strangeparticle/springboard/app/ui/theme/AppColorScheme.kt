package com.strangeparticle.springboard.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun appColorScheme() = lightColorScheme(
    primary = Color(0xFF1976D2), // PrimaryActionButton, SettingsSourceUserSettings
    onPrimary = Color.White, // button text on primary surfaces
    primaryContainer = Color(0xFF89B4FA), // ActiveCellIndicator
    surface = Color.White, // general backgrounds
    onSurface = Color(0xFF333333), // SettingsHeaderText, SettingsKeyText
    onSurfaceVariant = Color(0xFF666666), // SettingsColumnHeaderText, StatusBarText
    surfaceContainer = Color(0xFFF5F5F5), // RowHighlight, ColumnHighlight, CellHighlight, EmptyCellBackground, SettingsColumnBackground
    surfaceContainerHigh = Color(0xFFEEEEEE), // HeaderHoverBackground
    surfaceContainerLowest = Color(0xFFF8F8F6), // SettingsCardBackground
    surfaceContainerLow = Color(0xFFF0F0F0), // StatusBarBackground, SettingsSubDivider
    outline = Color(0xFFE0E0E0), // GridDividerColor, SettingsDivider
    outlineVariant = Color(0xFFDDDDDD), // GridBorderColor
    error = Color(0xFFEF9A9A), // ToastErrorBorder
    errorContainer = Color(0xFFFFEBEE), // ToastErrorBackground
    onErrorContainer = Color(0xFFC62828), // ToastErrorText
)
