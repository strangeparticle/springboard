package com.strangeparticle.springboard.app.ui.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.items.base.ListOfStringSettingsItem
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.ui.settings.ClearSettingButton
import com.strangeparticle.springboard.app.ui.settings.ProvenanceLabel
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Renders a list-of-strings setting with its values as a bullet list inside
 * the row. Doesn't use [SettingRowScaffold] because the values need a custom
 * full-width placement.
 *
 * The widget itself is read-only; mutation happens via dedicated actions
 * elsewhere (e.g. the "Use current tabs" affordance for
 * [com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting]).
 */
@Composable
internal fun ListOfStringSettingRowComposable(
    item: ListOfStringSettingsItem,
    viewModel: SettingsViewModel,
    itemAnnotation: ((String) -> String)? = null,
    extraContent: @Composable () -> Unit = {},
) {
    val effectiveSource = viewModel.getEffectiveSource(item)
    val hasUserChoice = effectiveSource == SettingsSource.USER_SETTINGS_FROM_SESSION ||
        effectiveSource == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE
    val values = viewModel.getResolvedValue(item)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
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
                ListOfStringValuesDisplay(values, itemAnnotation)
                extraContent()
                Spacer(modifier = Modifier.height(6.dp))
                ProvenanceLabel(
                    effectiveSource = effectiveSource,
                    runtimeEnvironment = viewModel.runtimeEnvironment,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            ClearSettingButton(
                enabled = hasUserChoice,
                fallbackSourceLabel = viewModel.fallbackSourceLabel(item),
                onClear = { viewModel.clearUserSetting(item) },
            )
        }
    }
}

@Composable
private fun ListOfStringValuesDisplay(
    values: List<String>,
    itemAnnotation: ((String) -> String)? = null,
) {
    val currentUiBrand = LocalUiBrand.current
    Column {
        if (values.isNotEmpty()) {
            for (item in values) {
                val annotation = itemAnnotation?.invoke(item)
                val displayText = if (annotation != null) "• [$annotation] $item" else "• $item"
                Text(
                    text = displayText,
                    fontSize = 12.sp,
                    color = currentUiBrand.customColors.settingsValueText,
                )
            }
        } else {
            Text(
                text = "(empty)",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = currentUiBrand.customColors.settingsNoValueText,
            )
        }
    }
}
