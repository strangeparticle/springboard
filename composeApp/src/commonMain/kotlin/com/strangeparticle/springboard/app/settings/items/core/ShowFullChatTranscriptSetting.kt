package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object ShowFullChatTranscriptSetting : BooleanSettingsItem() {
    override val id = "show_full_chat_transcript"
    override val displayName = "Show Full Chat Transcript (for Debug)"
    override val description = "Display the raw provider-side message log in the AI chat pane, including injected app-state snapshots and tool-result payloads that the normal view hides."
    override val group = SettingsGroup.DeveloperTools
    override val defaultValue = false
}
