package com.strangeparticle.springboard.app.luther.provider

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.provider.ProviderConfig
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/** Host-side counterpart to a luther AiProvider: declares the provider's Springboard
 *  settings, renders its rows, and projects resolved values into a typed ProviderConfig.
 *  This is the seam editio will later generalize. */
internal interface AiProviderSettingsAdaptor {
    val providerId: String
    fun settingsItems(): List<SettingsItem<*>>
    val settingsSection: @Composable (SettingsViewModel) -> Unit
    fun buildProviderConfig(settings: SettingsViewModel): ProviderConfig
}
