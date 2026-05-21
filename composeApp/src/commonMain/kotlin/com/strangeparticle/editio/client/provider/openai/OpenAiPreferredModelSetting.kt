package com.strangeparticle.editio.client.provider.openai

import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.settings.items.base.DropDownFromApiCallSettingsItem

internal object OpenAiPreferredModelSetting : DropDownFromApiCallSettingsItem() {
    override val id = "ai.openai.preferred_model"
    override val displayName = "OpenAI model"
    override val description = "Preferred OpenAI model id."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = ""

    override suspend fun loadOptions(context: SettingsItemContext): Result<List<DropDownOption>> = runCatching {
        val apiKey = context.get(OpenAiApiKeySetting).orEmpty()
        if (apiKey.isBlank()) return@runCatching emptyList()
        val client = OpenAiProvider.createClient(context)
        val models = client.listModels(apiKey)
        val preferredIds = OpenAiProvider.preferredModelIds()
        // Surface preferred models first, then the rest of the tool-capable list.
        val tooled = models.filter { it.supportsToolCalling }
        val preferred = preferredIds.mapNotNull { id -> tooled.firstOrNull { it.id == id } }
        val remainder = tooled.filterNot { it.id in preferredIds.toSet() }
        (preferred + remainder).map { DropDownOption(it.id, it.displayName ?: it.id) }
    }
}
