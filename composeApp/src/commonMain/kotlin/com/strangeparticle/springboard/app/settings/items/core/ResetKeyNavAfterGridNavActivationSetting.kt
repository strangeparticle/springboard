package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem

object ResetKeyNavAfterGridNavActivationSetting : BooleanSettingsItem() {
    override val id = "reset_key_nav_after_grid_nav_activation"
    override val displayName = "Reset keyNav drop-downs after activation via grid-nav"
    override val description = "When enabled, keyNav drop-downs are reset after activating a selection through the grid."
    override val group = SettingsGroup.General
    override val defaultValue = true
}
