package com.strangeparticle.springboard.app.ui.statusbar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.platform.formatTimestamp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand

@Composable
fun StatusBar(
    springboard: Springboard?,
    isReloading: Boolean,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val currentSpringboard = springboard ?: return
    val currentUiBrand = LocalUiBrand.current

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CommonUiConstants.StatusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onReload,
            enabled = !isReloading,
            modifier = Modifier.size(24.dp).testTag(TestTags.RELOAD_BUTTON)
        ) {
            Icon(
                imageVector = currentUiBrand.vectorImages.reload,
                contentDescription = if (isReloading) "Reloading" else "Reload",
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer(rotationZ = if (isReloading) rotation else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Source: ${currentSpringboard.source} @ ${formatTimestamp(currentSpringboard.lastLoadTime)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag(TestTags.STATUS_BAR_SOURCE),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = currentUiBrand.vectorImages.settings,
                contentDescription = "Settings",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
