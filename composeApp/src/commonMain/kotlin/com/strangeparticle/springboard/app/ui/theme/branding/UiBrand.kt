package com.strangeparticle.springboard.app.ui.theme.branding

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

/**
 * The final fully-built UI brand after all layers have been merged. This is the
 * runtime object the app actually uses. It holds resolved (non-nullable) theme
 * inputs and provides accessors for Compose Multiplatform theming.
 */
data class UiBrand(
    val colors: UiBrandColors,
    val typography: UiBrandTypography,
    val shapes: UiBrandShapes,
    val icons: UiBrandIcons,
) {
    /**
     * Builds a Material 3 [ColorScheme] from the resolved brand colors.
     * This is a placeholder that returns the default light color scheme;
     * the actual mapping will be implemented when the brand builder is wired up.
     */
    fun toColorScheme(): ColorScheme {
        return androidx.compose.material3.lightColorScheme()
    }

    /**
     * Builds a Material 3 [Typography] from the resolved brand typography.
     * Returns a default Typography with any brand overrides applied.
     */
    fun toTypography(): Typography {
        val defaultTypography = Typography()
        return Typography(
            displayLarge = typography.displayLarge ?: defaultTypography.displayLarge,
            displayMedium = typography.displayMedium ?: defaultTypography.displayMedium,
            displaySmall = typography.displaySmall ?: defaultTypography.displaySmall,
            headlineLarge = typography.headlineLarge ?: defaultTypography.headlineLarge,
            headlineMedium = typography.headlineMedium ?: defaultTypography.headlineMedium,
            headlineSmall = typography.headlineSmall ?: defaultTypography.headlineSmall,
            titleLarge = typography.titleLarge ?: defaultTypography.titleLarge,
            titleMedium = typography.titleMedium ?: defaultTypography.titleMedium,
            titleSmall = typography.titleSmall ?: defaultTypography.titleSmall,
            bodyLarge = typography.bodyLarge ?: defaultTypography.bodyLarge,
            bodyMedium = typography.bodyMedium ?: defaultTypography.bodyMedium,
            bodySmall = typography.bodySmall ?: defaultTypography.bodySmall,
            labelLarge = typography.labelLarge ?: defaultTypography.labelLarge,
            labelMedium = typography.labelMedium ?: defaultTypography.labelMedium,
            labelSmall = typography.labelSmall ?: defaultTypography.labelSmall,
        )
    }

    /**
     * Builds Material 3 [Shapes] from the resolved brand shapes.
     * Returns default shapes with any brand overrides applied.
     */
    fun toShapes(): Shapes {
        val defaultShapes = Shapes()
        return Shapes(
            small = shapes.small ?: defaultShapes.small,
            medium = shapes.medium ?: defaultShapes.medium,
            large = shapes.large ?: defaultShapes.large,
            extraLarge = shapes.extraLarge ?: defaultShapes.extraLarge,
        )
    }

    /**
     * Looks up an icon by its key. Returns null if the key has no icon
     * assigned in this brand.
     */
    fun iconFor(iconKey: UiBrandIconKey): UiBrandIconReference? {
        return icons.icons[iconKey]
    }
}
