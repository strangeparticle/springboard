package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object SurfaceAppleScriptErrorsSetting : BooleanSettingsItem() {
    override val id = "surface_applescript_errors"
    override val displayName = "Surface AppleScript errors"
    override val description = "When enabled, AppleScript failures are shown as error toasts instead of being silently swallowed."
    override val group = SettingsGroup.DesktopMacOS
    override val applicability = setOf(RuntimeEnvironment.DesktopOsx)
    override val defaultValue = false
}
