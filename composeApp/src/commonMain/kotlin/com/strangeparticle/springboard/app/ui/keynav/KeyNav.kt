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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

private const val MinWeightChars = 12

@Composable
fun KeyNav(
    viewModel: SpringboardViewModel,
    firstDropdownFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val resourceFocusRequester = remember { FocusRequester() }
    val environmentFocusRequester = remember { FocusRequester() }

    val appItems = viewModel.apps.map { it.id to it.name }
    val resourceItems = viewModel.resources.map { it.id to it.name }
    val environmentItems = viewModel.environments.map { it.id to it.name }

    fun longestNameWeight(items: List<Pair<String, String>>): Float {
        val longest = items.maxOfOrNull { it.second.length } ?: 0
        return maxOf(longest, MinWeightChars).toFloat()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dropdown 1: App
        MinimalDropdown(
            items = appItems,
            selectedId = viewModel.selectedAppId,
            enabledStates = viewModel.appEnabledStates,
            onSelect = { viewModel.selectApp(it) },
            focusRequester = firstDropdownFocusRequester,
            onTab = { resourceFocusRequester.requestFocus() },
            testTag = TestTags.APP_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(appItems)),
        )

        // Dropdown 2: Resource
        MinimalDropdown(
            items = resourceItems,
            selectedId = viewModel.selectedResourceId,
            enabledStates = viewModel.resourceEnabledStates,
            onSelect = { viewModel.selectResource(it) },
            focusRequester = resourceFocusRequester,
            onTab = { environmentFocusRequester.requestFocus() },
            testTag = TestTags.RESOURCE_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(resourceItems)),
        )

        // Dropdown 3: Environment
        MinimalDropdown(
            items = environmentItems,
            selectedId = viewModel.selectedEnvironmentId,
            enabledStates = viewModel.environments.associate { it.id to true },
            onSelect = { viewModel.selectEnvironment(it) },
            focusRequester = environmentFocusRequester,
            testTag = TestTags.ENVIRONMENT_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(environmentItems)),
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
    testTag: String? = null,
    modifier: Modifier = Modifier,
    onEnter: (() -> Unit)? = null,
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
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
