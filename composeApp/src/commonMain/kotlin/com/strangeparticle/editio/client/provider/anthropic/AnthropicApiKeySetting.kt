package com.strangeparticle.editio.client.provider.anthropic

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem

internal object AnthropicApiKeySetting : StringSettingsItem() {
    override val id = "ai.anthropic.api_key"
    override val displayName = "Anthropic API Key"
    override val description = "Used when the AI provider is Anthropic. The ANTHROPIC_API_KEY environment variable overrides this when set."
    override val group = SettingsGroup.AiAssistant
    override val envVarNameOverride = "ANTHROPIC_API_KEY"
    override val defaultValue = ""
    override val isSensitive = true
}
