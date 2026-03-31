package com.strangeparticle.springboard.app.ui.brand

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle.StrangeParticleBrand
import com.strangeparticle.springboard.app.ui.brand.infrastructure.UiBrand

val LocalUiBrand = compositionLocalOf<UiBrand> {
    error("No UiBrand provided. Wrap your UI in AppTheme or provide LocalUiBrand.")
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // Expose brand-only tokens (custom colors/icons) to the subtree while
    // MaterialTheme handles colorScheme/typography/shapes.
    CompositionLocalProvider(LocalUiBrand provides StrangeParticleBrand()) {
        val activeUiBrand = LocalUiBrand.current
        MaterialTheme(
            colorScheme = activeUiBrand.colorScheme,
            typography = activeUiBrand.typography,
            shapes = activeUiBrand.shapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                content()
            }
        }
    }
}
