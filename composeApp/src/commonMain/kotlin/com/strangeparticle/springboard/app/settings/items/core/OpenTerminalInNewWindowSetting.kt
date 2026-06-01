package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object OpenTerminalInNewWindowSetting : BooleanSettingsItem() {
    override val id = "open_terminal_in_new_window"
    override val displayName = "Open terminal activators in a new window"
    override val description = "When enabled, `term` activators open a new, dedicated terminal window. When disabled, the command opens in a new tab in the current terminal window (or a new window if none is open)."
    override val group = SettingsGroup.DesktopMacOS
    override val applicability = setOf(RuntimeEnvironment.DesktopOsx)
    override val defaultValue = false
}
