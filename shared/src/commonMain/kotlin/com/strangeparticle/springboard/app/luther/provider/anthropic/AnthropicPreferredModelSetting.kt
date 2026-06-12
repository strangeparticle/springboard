package com.strangeparticle.springboard.app.luther.provider.anthropic

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem

internal object AnthropicPreferredModelSetting : StringSettingsItem() {
    override val id = "ai.anthropic.preferred_model"
    override val displayName = "Anthropic model"
    override val description = "Preferred Anthropic model id."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = ""
}
