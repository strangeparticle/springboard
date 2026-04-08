package com.strangeparticle.springboard.app.ui.brand

import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.ui.brand.infrastructure.UiBrand

/**
 * A single entry in the [BrandRegistry]. Downstream forks register additional
 * entries here to make new brands selectable via the Active Brand setting.
 */
data class BrandRegistryEntry(
    val id: String,
    val displayName: String,
    val produceBrand: @Composable () -> UiBrand,
)
