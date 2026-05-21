package com.strangeparticle.springboard.app.ui.settings

import androidx.compose.runtime.Composable
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem
import com.strangeparticle.springboard.app.settings.items.base.DropDownFromApiCallSettingsItem
import com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem
import com.strangeparticle.springboard.app.settings.items.base.ListOfStringSettingsItem
import com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem
import com.strangeparticle.springboard.app.ui.settings.widget.BooleanSettingRowComposable
import com.strangeparticle.springboard.app.ui.settings.widget.DropDownFromApiCallSettingRowComposable
import com.strangeparticle.springboard.app.ui.settings.widget.DropDownSettingRowComposable
import com.strangeparticle.springboard.app.ui.settings.widget.ListOfStringSettingRowComposable
import com.strangeparticle.springboard.app.ui.settings.widget.StringSettingRowComposable
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * The framework's per-item row dispatcher. Picks the widget for [item] based on
 * its typed base class. Order matters: [DropDownSettingsItem] / [DropDownFromApiCallSettingsItem]
 * are checked before [StringSettingsItem] because both extend it.
 */
@Composable
fun SettingRowComposable(
    item: SettingsItem<*>,
    viewModel: SettingsViewModel,
) {
    when (item) {
        is BooleanSettingsItem -> BooleanSettingRowComposable(item, viewModel)
        is DropDownFromApiCallSettingsItem -> DropDownFromApiCallSettingRowComposable(item, viewModel)
        is DropDownSettingsItem -> DropDownSettingRowComposable(item, viewModel)
        is ListOfStringSettingsItem -> ListOfStringSettingRowComposable(item, viewModel)
        is StringSettingsItem -> StringSettingRowComposable(item, viewModel)
        else -> error("No SettingRowComposable for ${item::class.simpleName}")
    }
}
