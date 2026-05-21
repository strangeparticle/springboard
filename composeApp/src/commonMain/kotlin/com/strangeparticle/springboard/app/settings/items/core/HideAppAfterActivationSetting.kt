package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object HideAppAfterActivationSetting : BooleanSettingsItem() {
    override val id = "hide_app_after_activation"
    override val displayName = "Hide Springboard app after activation"
    override val description = "When enabled, the app hides itself after activating a cell, column, or row."
    override val group = SettingsGroup.DesktopMacOS
    override val applicability = setOf(RuntimeEnvironment.DesktopOsx)
    override val defaultValue = true
}
