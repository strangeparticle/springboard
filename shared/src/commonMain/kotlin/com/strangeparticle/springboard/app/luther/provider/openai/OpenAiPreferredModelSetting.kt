package com.strangeparticle.springboard.app.luther.provider.openai

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem

internal object OpenAiPreferredModelSetting : StringSettingsItem() {
    override val id = "ai.openai.preferred_model"
    override val displayName = "OpenAI model"
    override val description = "Preferred OpenAI model id."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = ""
}
