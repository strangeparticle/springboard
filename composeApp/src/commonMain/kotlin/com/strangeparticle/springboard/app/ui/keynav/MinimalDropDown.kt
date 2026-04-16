package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MinimalDropdown(
    items: List<Pair<String, String>>,
    selectedId: String?,
    enabledStates: Map<String, Boolean>,
    onSelect: (String?) -> Unit,
    focusRequester: FocusRequester,
    onTab: () -> Unit,
    onShiftTab: () -> Unit,
    onEscape: () -> Unit,
    testTag: String? = null,
    modifier: Modifier = Modifier,
    canActivateCoordinate: Boolean,
    onActivateCoordinate: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var typeaheadBuffer by remember { mutableStateOf("") }
    val selectedName = items.find { it.first == selectedId }?.second ?: KeyNavNoneOptionLabel

    fun handleTypeahead(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val hasModifier = event.isMetaPressed || event.isCtrlPressed || event.isAltPressed
        val char = keyToTypeaheadChar(event)
        if (char == null || hasModifier) return false

        typeaheadBuffer = appendTypeaheadBuffer(typeaheadBuffer, char)
        val matchId = findTypeaheadMatchId(typeaheadBuffer, items, enabledStates)

        if (matchId != null) {
            if (matchId == KeyNavNoneOptionId) {
                onSelect(null)
            } else {
                onSelect(matchId)
            }
            expanded = false
        }

        return true
    }

    LaunchedEffect(typeaheadBuffer) {
        if (typeaheadBuffer.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            typeaheadBuffer = ""
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val currentUiBrand = LocalUiBrand.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused)
                        currentUiBrand.customColors.keyNavFocusIndicator
                    else
                        currentUiBrand.customColors.keyNavFocusIndicatorUnfocused,
                    shape = RectangleShape
                )
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .let { if (testTag != null) it.testTag(testTag) else it }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Tab -> {
                                expanded = false
                                if (event.isShiftPressed) {
                                    onShiftTab()
                                } else {
                                    onTab()
                                }
                                true
                            }

                            Key.Enter -> {
                                if (canActivateCoordinate) {
                                    expanded = false
                                    onActivateCoordinate()
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.Escape -> {
                                expanded = false
                                onEscape()
                                true
                            }

                            else -> handleTypeahead(event)
                        }
                    } else false
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = selectedName,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .focusable()
                .onPreviewKeyEvent { handleTypeahead(it) },
        ) {
            DropdownMenuItem(
                modifier = Modifier.onPreviewKeyEvent { handleTypeahead(it) },
                text = { Text(KeyNavNoneOptionLabel, fontSize = 13.sp) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                enabled = true,
            )

            HorizontalDivider()

            items.forEach { (id, name) ->
                val isEnabled = enabledStates[id] != false
                DropdownMenuItem(
                    modifier = Modifier.onPreviewKeyEvent { handleTypeahead(it) },
                    text = { Text(name, fontSize = 13.sp) },
                    onClick = {
                        if (isEnabled) {
                            onSelect(id)
                            expanded = false
                        }
                    },
                    enabled = isEnabled
                )
            }
        }
    }
}
