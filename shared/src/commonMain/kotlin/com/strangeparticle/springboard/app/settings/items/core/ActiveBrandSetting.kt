package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry

object ActiveBrandSetting : DropDownSettingsItem() {
    override val id = "active_brand"
    override val displayName = "Active Brand"
    override val description = "Choose the visual brand theme for the app."
    override val group = SettingsGroup.General
    override val defaultValue: String = BrandRegistry.defaultBrand.id
    override val options: List<DropDownOption> =
        BrandRegistry.entries.map { DropDownOption(it.id, it.displayName) }
}
