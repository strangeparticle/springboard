package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.theme.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

@Composable
fun KeyNav(
    viewModel: SpringboardViewModel,
    firstDropdownFocusRequester: FocusRequester
) {
    val resourceFocusRequester = remember { FocusRequester() }
    val environmentFocusRequester = remember { FocusRequester() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dropdown 1: App
        MinimalDropdown(
            items = viewModel.apps.map { it.id to it.name },
            selectedId = viewModel.selectedAppId,
            enabledStates = viewModel.appEnabledStates,
            onSelect = { viewModel.selectApp(it) },
            focusRequester = firstDropdownFocusRequester,
            onTab = { resourceFocusRequester.requestFocus() }
        )

        // Dropdown 2: Resource
        MinimalDropdown(
            items = viewModel.resources.map { it.id to it.name },
            selectedId = viewModel.selectedResourceId,
            enabledStates = viewModel.resourceEnabledStates,
            onSelect = { viewModel.selectResource(it) },
            focusRequester = resourceFocusRequester,
            onTab = { environmentFocusRequester.requestFocus() }
        )

        // Dropdown 3: Environment
        MinimalDropdown(
            items = viewModel.environments.map { it.id to it.name },
            selectedId = viewModel.selectedEnvironmentId,
            enabledStates = viewModel.environments.associate { it.id to true },
            onSelect = { viewModel.selectEnvironment(it) },
            focusRequester = environmentFocusRequester,
            onTab = {
                if (viewModel.isActivateEnabled) {
                    viewModel.activateCurrentSelection()
                }
                firstDropdownFocusRequester.requestFocus()
            },
            onEnter = {
                if (viewModel.isActivateEnabled) {
                    viewModel.activateCurrentSelection()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalDropdown(
    items: List<Pair<String, String>>,
    selectedId: String?,
    enabledStates: Map<String, Boolean>,
    onSelect: (String) -> Unit,
    focusRequester: FocusRequester,
    onTab: () -> Unit,
    onEnter: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var typeaheadBuffer by remember { mutableStateOf("") }
    val selectedName = items.find { it.first == selectedId }?.second ?: ""

    // Clear typeahead buffer after a delay
    LaunchedEffect(typeaheadBuffer) {
        if (typeaheadBuffer.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            typeaheadBuffer = ""
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(CommonUiConstants.DropdownWidth)
    ) {
        Box(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .height(34.dp)
                .background(Color.White, RectangleShape)
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) Color(0xFF1E6FFF) else Color.White.copy(alpha = 0.3f),
                    shape = RectangleShape
                )
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Tab -> {
                                expanded = false
                                onTab()
                                true
                            }
                            Key.Enter -> {
                                if (!expanded) {
                                    expanded = false
                                    onEnter?.invoke()
                                    true
                                } else false
                            }
                            else -> {
                                val char = event.utf16CodePoint.toChar()
                                val hasModifier = event.isMetaPressed || event.isCtrlPressed || event.isAltPressed
                                if (char.isLetterOrDigit() && !hasModifier) {
                                    typeaheadBuffer += char.lowercaseChar()
                                    val match = items.find { (_, name) ->
                                        name.lowercase().startsWith(typeaheadBuffer)
                                    }
                                    if (match != null && enabledStates[match.first] != false) {
                                        onSelect(match.first)
                                    }
                                    true
                                } else false
                            }
                        }
                    } else false
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = selectedName,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { (id, name) ->
                val isEnabled = enabledStates[id] != false
                DropdownMenuItem(
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
