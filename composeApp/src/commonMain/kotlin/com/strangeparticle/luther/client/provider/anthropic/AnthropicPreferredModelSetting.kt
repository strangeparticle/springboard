package com.strangeparticle.luther.client.provider.anthropic

import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.settings.items.base.DropDownFromApiCallSettingsItem

internal object AnthropicPreferredModelSetting : DropDownFromApiCallSettingsItem() {
    override val id = "ai.anthropic.preferred_model"
    override val displayName = "Anthropic model"
    override val description = "Preferred Anthropic model id."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = ""

    override suspend fun loadOptions(context: SettingsItemContext): Result<List<DropDownOption>> = runCatching {
        val apiKey = context.get(AnthropicApiKeySetting).orEmpty()
        if (apiKey.isBlank()) return@runCatching emptyList()
        val client = AnthropicProvider.createClient(context)
        val models = client.listModels(apiKey)
        val preferredIds = AnthropicProvider.preferredModelIds()
        val tooled = models.filter { it.supportsToolCalling }
        val preferred = preferredIds.mapNotNull { id -> tooled.firstOrNull { it.id == id } }
        val remainder = tooled.filterNot { it.id in preferredIds.toSet() }
        (preferred + remainder).map { DropDownOption(it.id, it.displayName ?: it.id) }
    }
}
