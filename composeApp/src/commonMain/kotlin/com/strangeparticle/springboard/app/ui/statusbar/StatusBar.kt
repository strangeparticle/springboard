package com.strangeparticle.springboard.app.ui.statusbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.platform.formatTimestamp
import com.strangeparticle.springboard.app.ui.theme.CommonUiConstants
import com.strangeparticle.springboard.app.ui.theme.StatusBarBackground
import com.strangeparticle.springboard.app.ui.theme.StatusBarText

@Composable
fun StatusBar(
    springboard: Springboard?,
    isReloading: Boolean,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val currentSpringboard = springboard ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CommonUiConstants.StatusBarHeight)
            .background(StatusBarBackground)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onReload,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Reload",
                modifier = Modifier.size(16.dp),
                tint = StatusBarText
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Source: ${currentSpringboard.source} @ ${formatTimestamp(currentSpringboard.lastLoadTime)}",
            fontSize = 11.sp,
            color = StatusBarText,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(16.dp),
                tint = StatusBarText
            )
        }
    }
}
