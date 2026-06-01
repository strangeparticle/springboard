package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.platform.PreferredTerminal
import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem

object PreferredTerminalSetting : DropDownSettingsItem() {
    override val id = "preferred_terminal"
    override val displayName = "Preferred terminal"
    override val description = "Which terminal app `term` activators open. iTerm falls back to Terminal if it isn't installed."
    override val group = SettingsGroup.DesktopMacOS
    override val applicability = setOf(RuntimeEnvironment.DesktopOsx)
    override val defaultValue: String = PreferredTerminal.TerminalApp.id
    override val options: List<DropDownOption> = listOf(
        DropDownOption(PreferredTerminal.TerminalApp.id, "Terminal"),
        DropDownOption(PreferredTerminal.ITerm.id, "iTerm"),
    )
}
