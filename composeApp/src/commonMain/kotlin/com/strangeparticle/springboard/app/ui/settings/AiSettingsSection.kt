package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.settings.ai.PreferredAiModels
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * AI settings UX:
 *  - Provider dropdown (first). Default `None`. Changing provider clears any in-memory
 *    fetched model list and shifts focus to the API key field.
 *  - Single API key field. Maps to the per-provider stored key (so switching providers
 *    preserves each provider's key). Disabled when provider == None.
 *  - Model dropdown. Disabled until provider + key both have non-empty values. When both
 *    become valid the section auto-fetches the model list. A refresh icon re-runs the
 *    fetch on demand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiSettingsSection(
    viewModel: SettingsViewModel,
    environmentVariables: Map<String, String> = emptyMap(),
    fetchModels: suspend (AiProvider, String) -> List<AiClientModelInfo> = { _, _ -> emptyList() },
) {
    val scope = rememberCoroutineScope()
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<AiClientModelInfo>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    val apiKeyFocusRequester = remember { FocusRequester() }

    val selectedProvider = AiProvider.fromId(
        viewModel.getResolvedValue(SettingsKey.AI_PROVIDER) as? String ?: AiProvider.None.id,
    )
    val selectedModelId = viewModel.getResolvedValue(SettingsKey.AI_MODEL) as? String ?: ""
    val persistedApiKey = when (selectedProvider) {
        AiProvider.OpenAi -> viewModel.getResolvedValue(SettingsKey.AI_OPENAI_API_KEY) as? String ?: ""
        AiProvider.Anthropic -> viewModel.getResolvedValue(SettingsKey.AI_ANTHROPIC_API_KEY) as? String ?: ""
        AiProvider.None -> ""
    }
    val envOverrideKey = selectedProvider.envVarName()?.let { name ->
        environmentVariables[name]?.takeIf { it.isNotEmpty() }
    }
    val displayedApiKey = envOverrideKey ?: persistedApiKey
    val effectiveApiKey = (envOverrideKey ?: persistedApiKey).trim()
    val apiKeyEnabled = selectedProvider != AiProvider.None && envOverrideKey == null
    val canFetchModels = selectedProvider != AiProvider.None && effectiveApiKey.isNotEmpty()

    // Auto-fetch when (provider + key) becomes valid. Re-runs whenever either changes.
    LaunchedEffect(selectedProvider, effectiveApiKey) {
        if (!canFetchModels) {
            availableModels = emptyList()
            return@LaunchedEffect
        }
        runFetch(
            provider = selectedProvider,
            apiKey = effectiveApiKey,
            fetchModels = fetchModels,
            setFetching = { isFetching = it },
            setError = { fetchError = it },
            setModels = { availableModels = it },
            viewModel = viewModel,
            currentSelectedModelId = selectedModelId,
        )
    }

    // Heading lives outside the Surface so vertical padding around the AI section matches
    // the rest of the screen: SettingsGroupSection renders its heading outside the per-row
    // cards, and SettingRow uses padding(horizontal = 16.dp, vertical = 14.dp) inside its
    // Surface. Matching both keeps the gap between this section and adjacent groups equal
    // to the inter-group gap everywhere else.
    Text(
        text = "AI Assistant",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(10.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            // ── Provider dropdown ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expandedProvider,
                    onExpandedChange = { expandedProvider = it },
                    modifier = Modifier.width(260.dp).testTag(TestTags.AI_PROVIDER_DROPDOWN),
                ) {
                    DropdownAnchor(
                        label = selectedProvider.displayName,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProvider,
                        onDismissRequest = { expandedProvider = false },
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    viewModel.setUserSetting(SettingsKey.AI_PROVIDER, provider.id)
                                    viewModel.clearUserSetting(SettingsKey.AI_MODEL)
                                    availableModels = emptyList()
                                    fetchError = null
                                    expandedProvider = false
                                    if (provider != AiProvider.None) {
                                        scope.launch { apiKeyFocusRequester.requestFocus() }
                                    }
                                },
                            )
                        }
                    }
                }
                ClearAiSettingButton(
                    enabled = selectedProvider != AiProvider.None,
                    testTag = TestTags.AI_PROVIDER_CLEAR_BUTTON,
                    contentDescription = "Clear AI provider",
                    onClear = { viewModel.clearUserSetting(SettingsKey.AI_PROVIDER) },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── API key field ─────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = displayedApiKey,
                    onValueChange = { newValue ->
                        when (selectedProvider) {
                            AiProvider.OpenAi -> viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, newValue)
                            AiProvider.Anthropic -> viewModel.setUserSetting(SettingsKey.AI_ANTHROPIC_API_KEY, newValue)
                            AiProvider.None -> { /* field is disabled */ }
                        }
                    },
                    label = { Text("API key") },
                    singleLine = true,
                    enabled = apiKeyEnabled,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(apiKeyFocusRequester)
                        .testTag(TestTags.AI_API_KEY_FIELD),
                )
                TextButton(
                    onClick = { showApiKey = !showApiKey },
                    enabled = selectedProvider != AiProvider.None,
                    modifier = Modifier.testTag(TestTags.AI_API_KEY_VISIBILITY_BUTTON),
                ) {
                    Text(if (showApiKey) "Hide" else "Show", fontSize = 12.sp)
                }
                ClearAiSettingButton(
                    enabled = selectedProvider != AiProvider.None && persistedApiKey.isNotEmpty(),
                    testTag = TestTags.AI_API_KEY_CLEAR_BUTTON,
                    contentDescription = "Clear AI API key",
                    onClear = {
                        when (selectedProvider) {
                            AiProvider.OpenAi -> viewModel.clearUserSetting(SettingsKey.AI_OPENAI_API_KEY)
                            AiProvider.Anthropic -> viewModel.clearUserSetting(SettingsKey.AI_ANTHROPIC_API_KEY)
                            AiProvider.None -> {}
                        }
                    },
                )
            }
            if (envOverrideKey != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Using ${selectedProvider.envVarName()} from environment",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Model dropdown + refresh ──────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                val modelEnabled = canFetchModels && availableModels.isNotEmpty()
                ExposedDropdownMenuBox(
                    expanded = expandedModel && modelEnabled,
                    onExpandedChange = { if (modelEnabled) expandedModel = it },
                    modifier = Modifier
                        .width(260.dp)
                        .alpha(if (modelEnabled) 1f else 0.55f)
                        .testTag(TestTags.AI_MODEL_DROPDOWN),
                ) {
                    val selectedModelLabel = availableModels.firstOrNull { it.id == selectedModelId }
                        ?.let { it.displayName ?: it.id }
                        ?: selectedModelId.ifBlank {
                            when {
                                !canFetchModels -> "Choose provider and enter API key"
                                isFetching -> "Loading models…"
                                else -> "No model selected"
                            }
                        }
                    DropdownAnchor(
                        label = selectedModelLabel,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModel && modelEnabled,
                        onDismissRequest = { expandedModel = false },
                    ) {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName ?: model.id) },
                                onClick = {
                                    viewModel.setUserSetting(SettingsKey.AI_MODEL, model.id)
                                    expandedModel = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        scope.launch {
                            runFetch(
                                provider = selectedProvider,
                                apiKey = effectiveApiKey,
                                fetchModels = fetchModels,
                                setFetching = { isFetching = it },
                                setError = { fetchError = it },
                                setModels = { availableModels = it },
                                viewModel = viewModel,
                                currentSelectedModelId = selectedModelId,
                            )
                        }
                    },
                    enabled = canFetchModels && !isFetching,
                    modifier = Modifier.size(32.dp).testTag(TestTags.AI_REFRESH_MODELS_BUTTON),
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh model list",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                ClearAiSettingButton(
                    enabled = selectedModelId.isNotEmpty(),
                    testTag = TestTags.AI_MODEL_CLEAR_BUTTON,
                    contentDescription = "Clear AI model",
                    onClear = { viewModel.clearUserSetting(SettingsKey.AI_MODEL) },
                )
            }
            fetchError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ClearAiSettingButton(
    enabled: Boolean,
    testTag: String,
    contentDescription: String,
    onClear: () -> Unit,
) {
    // Matches ClearSettingButton in SettingsScreen.kt: a 28dp Box with a 14dp Close icon.
    // We avoid IconButton because Material 3 forces a ~40dp interactive surface around
    // the icon, which makes this button visibly larger than the per-setting clear icons.
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(if (enabled) Modifier.clickable(role = Role.Button) { onClear() } else Modifier)
            .testTag(testTag),
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

private suspend fun runFetch(
    provider: AiProvider,
    apiKey: String,
    fetchModels: suspend (AiProvider, String) -> List<AiClientModelInfo>,
    setFetching: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setModels: (List<AiClientModelInfo>) -> Unit,
    viewModel: SettingsViewModel,
    currentSelectedModelId: String,
) {
    if (provider == AiProvider.None || apiKey.isEmpty()) return
    setFetching(true)
    setError(null)
    try {
        println("[Springboard] AI fetch models: provider=${provider.id}")
        val models = fetchModels(provider, apiKey)
        setModels(models)
        println("[Springboard] AI fetch models: got ${models.size} models")
        val currentInList = models.any { it.id == currentSelectedModelId }
        if (!currentInList) {
            PreferredAiModels.selectPreferred(provider, models)?.let { picked ->
                viewModel.setUserSetting(SettingsKey.AI_MODEL, picked.id)
            }
        }
    } catch (e: Exception) {
        setError(e.message ?: "Failed to fetch models")
        println("[Springboard] AI fetch models failed: ${e.message}")
    } finally {
        setFetching(false)
    }
}

@Composable
private fun DropdownAnchor(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

private fun AiProvider.envVarName(): String? = when (this) {
    AiProvider.OpenAi -> "OPENAI_API_KEY"
    AiProvider.Anthropic -> "ANTHROPIC_API_KEY"
    AiProvider.None -> null
}
