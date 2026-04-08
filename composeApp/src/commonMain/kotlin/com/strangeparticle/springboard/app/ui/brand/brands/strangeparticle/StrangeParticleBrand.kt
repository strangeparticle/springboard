package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.ui.brand.infrastructure.UiBrand

/**
 * Strange Particle brand, light variant.
 */
@Composable
fun strangeParticleLightBrand(): UiBrand = UiBrand(
    colorScheme = strangeParticleLightColorScheme(),
    customColors = StrangeParticleCustomColorsLightTheme,
    typography = StrangeParticleTypography(),
    shapes = Shapes(),  // we don't currently use any custom shapes
    vectorImages = StrangeParticleImageVectors,
    drawableResources = StrangeParticleImagePainters,
)

/**
 * Strange Particle brand, dark variant.
 */
@Composable
fun strangeParticleDarkBrand(): UiBrand = UiBrand(
    colorScheme = strangeParticleDarkColorScheme(),
    customColors = StrangeParticleCustomColorsDarkTheme,
    typography = StrangeParticleTypography(),
    shapes = Shapes(),
    vectorImages = StrangeParticleImageVectors,
    drawableResources = StrangeParticleImagePainters,
)
