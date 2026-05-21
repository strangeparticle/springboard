package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object OpenUrlsInNewWindowSingleSetting : BooleanSettingsItem() {
    override val id = "open_urls_in_new_window_single"
    override val displayName = "Open URLs in new browser window for single selections"
    override val description = "When enabled, single-cell activations open URLs in a new browser window."
    override val group = SettingsGroup.DesktopMacOS
    override val applicability = setOf(RuntimeEnvironment.DesktopOsx)
    override val defaultValue = true
}
