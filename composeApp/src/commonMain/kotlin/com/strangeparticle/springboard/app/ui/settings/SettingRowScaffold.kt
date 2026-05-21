package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Shared visual container used by every per-base setting row composable.
 * Renders the item's display name + description + source label on the left,
 * the type-specific [widgetContent] on the right, and the clear-user-choice
 * button at the end.
 */
@Composable
internal fun SettingRowScaffold(
    item: SettingsItem<*>,
    viewModel: SettingsViewModel,
    widgetContent: @Composable () -> Unit,
) {
    val effectiveSource = viewModel.getEffectiveSource(item)
    val hasUserChoice = effectiveSource == SettingsSource.USER_SETTINGS_FROM_SESSION ||
        effectiveSource == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.id,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = LocalUiBrand.current.customColors.settingsDescriptionText,
                )
                Spacer(modifier = Modifier.height(6.dp))
                ProvenanceLabel(
                    effectiveSource = effectiveSource,
                    runtimeEnvironment = viewModel.runtimeEnvironment,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            widgetContent()
            Spacer(modifier = Modifier.width(4.dp))
            ClearSettingButton(
                enabled = hasUserChoice,
                fallbackSourceLabel = viewModel.fallbackSourceLabel(item),
                onClear = { viewModel.clearUserSetting(item) },
            )
        }
    }
}

@Composable
internal fun ClearSettingButton(
    enabled: Boolean,
    fallbackSourceLabel: String,
    onClear: () -> Unit,
) {
    val contentDescription = if (enabled) {
        "Clear your choice — will use $fallbackSourceLabel value"
    } else {
        "No user choice to clear"
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(if (enabled) Modifier.clickable(role = Role.Button) { onClear() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            },
        )
    }
}

@Composable
internal fun ProvenanceLabel(
    effectiveSource: SettingsSource,
    runtimeEnvironment: com.strangeparticle.springboard.app.settings.RuntimeEnvironment,
) {
    Text(
        text = "Source: ${effectiveSource.displayLabel(runtimeEnvironment).lowercase()}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

/** Used by composables that need a small button (e.g. refresh on async dropdowns). */
internal val SettingRowSmallButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
internal val SettingRowWidgetWidth = 260.dp
internal val SettingRowSmallWidgetHeight = 36.dp
