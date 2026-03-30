package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import com.strangeparticle.springboard.app.ui.brand.infrastructure.VectorImages

/**
 * Default vector image values for the StrangeParticle brand.
 */
val StrangeParticleImageVectors = VectorImages(
    backNavigation = Icons.AutoMirrored.Filled.ArrowBack,
    settings = Icons.Default.Settings,
    reload = Icons.Default.Refresh,
    copy = Icons.Default.ContentCopy,
    copyConfirmed = Icons.Default.Check,
    dismiss = Icons.Default.Close,
    severityError = Icons.Outlined.Warning,
    severityWarning = Icons.Outlined.Info,
    severityInfo = Icons.Outlined.Info,
)
