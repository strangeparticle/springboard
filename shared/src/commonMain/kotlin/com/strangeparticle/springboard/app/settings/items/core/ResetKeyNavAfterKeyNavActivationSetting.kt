package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object ResetKeyNavAfterKeyNavActivationSetting : BooleanSettingsItem() {
    override val id = "reset_key_nav_after_key_nav_activation"
    override val displayName = "Reset keyNav drop-downs after activation via keyNav"
    override val description = "When enabled, keyNav drop-downs are reset after activating a selection through keyNav."
    override val group = SettingsGroup.General
    override val defaultValue = true
}
