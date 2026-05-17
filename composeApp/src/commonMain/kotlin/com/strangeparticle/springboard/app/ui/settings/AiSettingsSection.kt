package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.settings.ai.PreferredAiModels
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
    val selectedProvider = AiProvider.fromId(viewModel.getResolvedValue(SettingsKey.AI_PROVIDER) as? String ?: AiProvider.None.id)
    val selectedModelId = viewModel.getResolvedValue(SettingsKey.AI_MODEL) as? String ?: ""
    val openAiKey = viewModel.getResolvedValue(SettingsKey.AI_OPENAI_API_KEY) as? String ?: ""
    val anthropicKey = viewModel.getResolvedValue(SettingsKey.AI_ANTHROPIC_API_KEY) as? String ?: ""
    val envOverride = selectedProvider.envVarName()?.let { envName ->
        environmentVariables[envName]?.trim()?.takeIf { it.isNotEmpty() }?.let { envName }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("AI Assistant", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))

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
                                expandedProvider = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = openAiKey,
                onValueChange = { viewModel.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, it) },
                label = { Text("OpenAI API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.AI_OPENAI_API_KEY_FIELD),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = anthropicKey,
                onValueChange = { viewModel.setUserSetting(SettingsKey.AI_ANTHROPIC_API_KEY, it) },
                label = { Text("Anthropic API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.AI_ANTHROPIC_API_KEY_FIELD),
            )
            if (envOverride != null) {
                Spacer(Modifier.height(6.dp))
                Text("Using $envOverride from environment", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        scope.launch {
                            fetchError = null
                            val apiKey = resolvedApiKey(selectedProvider, environmentVariables, openAiKey, anthropicKey)
                            if (apiKey == null) {
                                fetchError = "Enter an API key first."
                                return@launch
                            }
                            try {
                                val models = fetchModels(selectedProvider, apiKey)
                                availableModels = models
                                PreferredAiModels.selectPreferred(selectedProvider, models)?.let { model ->
                                    viewModel.setUserSetting(SettingsKey.AI_MODEL, model.id)
                                }
                            } catch (e: Exception) {
                                fetchError = e.message ?: "Failed to fetch models"
                            }
                        }
                    },
                    enabled = selectedProvider != AiProvider.None,
                    modifier = Modifier.testTag(TestTags.AI_FETCH_MODELS_BUTTON),
                ) {
                    Text("Fetch models")
                }
                Spacer(Modifier.width(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedModel,
                    onExpandedChange = { expandedModel = it && availableModels.isNotEmpty() },
                    modifier = Modifier.width(260.dp).testTag(TestTags.AI_MODEL_DROPDOWN),
                ) {
                    DropdownAnchor(
                        label = availableModels.firstOrNull { it.id == selectedModelId }?.displayName ?: selectedModelId.ifBlank { "No model selected" },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModel && availableModels.isNotEmpty(),
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
            }
            fetchError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
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
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

private fun resolvedApiKey(
    provider: AiProvider,
    environmentVariables: Map<String, String>,
    openAiKey: String,
    anthropicKey: String,
): String? {
    val envName = provider.envVarName()
    if (envName != null) {
        environmentVariables[envName]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return when (provider) {
        AiProvider.OpenAi -> openAiKey
        AiProvider.Anthropic -> anthropicKey
        AiProvider.None -> ""
    }.trim().takeIf { it.isNotEmpty() }
}

private fun AiProvider.envVarName(): String? = when (this) {
    AiProvider.OpenAi -> "OPENAI_API_KEY"
    AiProvider.Anthropic -> "ANTHROPIC_API_KEY"
    AiProvider.None -> null
}
