package com.strangeparticle.springboard.app.ui.theme.branding

/**
 * Represents one layer in the UI brand stack. Each field is nullable so that
 * a layer only needs to specify the aspects it wants to override. During brand
 * building, layers are merged top-down: later layers override earlier ones for
 * any non-null fields.
 */
data class UiBrandDefinition(
    val colors: UiBrandColors? = null,
    val typography: UiBrandTypography? = null,
    val shapes: UiBrandShapes? = null,
    val icons: UiBrandIcons? = null,
)
