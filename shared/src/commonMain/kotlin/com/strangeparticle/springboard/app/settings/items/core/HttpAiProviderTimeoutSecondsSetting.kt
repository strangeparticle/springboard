package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.IntSettingsItem

object HttpAiProviderTimeoutSecondsSetting : IntSettingsItem() {
    override val id = "http.ai_provider_timeout_seconds"
    override val displayName = "AI Provider HTTP Timeout"
    override val description = "Maximum time, in seconds, for AI provider model listing and chat requests."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = 180
    override val minimumValue = 1
    override val maximumValue = 600
}
