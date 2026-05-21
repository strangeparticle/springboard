package com.strangeparticle.editio.client.provider.openai

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem

internal object OpenAiApiKeySetting : StringSettingsItem() {
    override val id = "ai.openai.api_key"
    override val displayName = "OpenAI API Key"
    override val description = "Used when the AI provider is OpenAI. The OPENAI_API_KEY environment variable overrides this when set."
    override val group = SettingsGroup.AiAssistant
    override val envVarNameOverride = "OPENAI_API_KEY"
    override val defaultValue = ""
    override val isSensitive = true
}
