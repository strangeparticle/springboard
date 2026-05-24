package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.IntSettingsItem

object HttpContentTimeoutSecondsSetting : IntSettingsItem() {
    override val id = "http.content_timeout_seconds"
    override val displayName = "Content HTTP Timeout"
    override val description = "Maximum time, in seconds, for opening Springboard content from network sources."
    override val group = SettingsGroup.General
    override val defaultValue = 30
    override val minimumValue = 1
    override val maximumValue = 600
}
