package com.strangeparticle.springboard.app.ui.statusbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.displayLabel

@Composable
fun ZoomDropdown(
    zoomSelection: GridZoomSelection,
    onZoomSelectionChange: (GridZoomSelection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.testTag(TestTags.ZOOM_DROPDOWN)) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.height(24.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        ) {
            Text(
                text = zoomSelection.displayLabel(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GridZoomSelection.presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = preset.displayLabel(),
                            fontSize = 12.sp,
                        )
                    },
                    onClick = {
                        onZoomSelectionChange(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}
