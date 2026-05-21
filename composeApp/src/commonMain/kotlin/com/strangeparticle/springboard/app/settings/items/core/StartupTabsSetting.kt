package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.ListOfStringSettingsItem

object StartupTabsSetting : ListOfStringSettingsItem() {
    override val id = "startup_tabs"
    override val displayName = "Startup Tabs"
    override val description = "Springboard files or URLs to open as tabs on launch."
    override val group = SettingsGroup.General
    override val defaultValue: List<String> = emptyList()
}
