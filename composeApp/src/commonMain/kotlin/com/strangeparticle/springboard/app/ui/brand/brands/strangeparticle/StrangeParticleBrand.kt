package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.ui.brand.infrastructure.UiBrand

/**
 * The default UI brand for Springboard.
 */
@Composable
fun StrangeParticleBrand(): UiBrand = UiBrand(
    colorScheme = strangeParticleColorScheme(),
    customColors = StrangeParticleCustomColors,
    typography = StrangeParticleTypography(),
    shapes = Shapes(),  // we don't currently use any custom shapes
    vectorImages = StrangeParticleImageVectors,
    drawableResources = StrangeParticleImagePainters,
)
