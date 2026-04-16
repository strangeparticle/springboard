package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

private const val MinWeightChars = 12
private const val MaxWeightChars = 30

@Composable
fun KeyNav(
    viewModel: SpringboardViewModel,
    modifier: Modifier = Modifier,
) {
    val appDropdownFocusRequester = remember { FocusRequester() }
    val resourceFocusRequester = remember { FocusRequester() }
    val environmentFocusRequester = remember { FocusRequester() }

    // Observe the view-model-owned focus-request flag and route it to the app dropdown's
    // FocusRequester. Startup load, toast dismissal, escape reset, and window-refocus all
    // toggle this flag via viewModel.requestFocusAppDropdown()
    LaunchedEffect(viewModel.focusAppDropdownRequested) {
        if (viewModel.focusAppDropdownRequested) {
            try { appDropdownFocusRequester.requestFocus() } catch (_: Exception) {}
            viewModel.focusAppDropdownRequested = false
        }
    }

    fun requestFocusForDropDown(dropDown: KeyNavDropDown) {
        when (dropDown) {
            KeyNavDropDown.APP -> appDropdownFocusRequester.requestFocus()
            KeyNavDropDown.RESOURCE -> resourceFocusRequester.requestFocus()
            KeyNavDropDown.ENVIRONMENT -> environmentFocusRequester.requestFocus()
        }
    }

    fun onEscape() {
        viewModel.resetKeyNavSelections()
        viewModel.requestFocusAppDropdown()
    }

    val appItems = viewModel.apps.map { it.id to it.name }
    val resourceItems = viewModel.resources.map { it.id to it.name }
    val environmentItems = viewModel.environments.map { it.id to it.name }

    fun longestNameWeight(items: List<Pair<String, String>>): Float {
        val longest = items.maxOfOrNull { it.second.length } ?: 0
        return longest.coerceIn(MinWeightChars, MaxWeightChars).toFloat()
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
            focusRequester = appDropdownFocusRequester,
            onTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.APP, isShiftPressed = false)
                )
            },
            onShiftTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.APP, isShiftPressed = true)
                )
            },
            onEscape = { onEscape() },
            testTag = TestTags.APP_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(appItems)),
            canActivateCoordinate = viewModel.isActivateEnabled,
            onActivateCoordinate = { viewModel.activateCurrentSelection() },
        )

        // Dropdown 2: Resource
        MinimalDropdown(
            items = resourceItems,
            selectedId = viewModel.selectedResourceId,
            enabledStates = viewModel.resourceEnabledStates,
            onSelect = { viewModel.selectResource(it) },
            focusRequester = resourceFocusRequester,
            onTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.RESOURCE, isShiftPressed = false)
                )
            },
            onShiftTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.RESOURCE, isShiftPressed = true)
                )
            },
            onEscape = { onEscape() },
            testTag = TestTags.RESOURCE_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(resourceItems)),
            canActivateCoordinate = viewModel.isActivateEnabled,
            onActivateCoordinate = { viewModel.activateCurrentSelection() },
        )

        // Dropdown 3: Environment
        MinimalDropdown(
            items = environmentItems,
            selectedId = viewModel.selectedEnvironmentId,
            enabledStates = viewModel.environmentEnabledStates,
            onSelect = { viewModel.selectEnvironment(it) },
            focusRequester = environmentFocusRequester,
            onTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.ENVIRONMENT, isShiftPressed = false)
                )
            },
            onShiftTab = {
                requestFocusForDropDown(
                    determineNextFocusDropDownForTabKeypress(KeyNavDropDown.ENVIRONMENT, isShiftPressed = true)
                )
            },
            onEscape = { onEscape() },
            testTag = TestTags.ENVIRONMENT_DROPDOWN,
            modifier = Modifier.weight(longestNameWeight(environmentItems)),
            canActivateCoordinate = viewModel.isActivateEnabled,
            onActivateCoordinate = { viewModel.activateCurrentSelection() },
        )
    }
}
