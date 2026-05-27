package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.luther.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem

/**
 * The one app-wide "which AI provider is active" knob. Options are built once
 * at class-load from [AiProviderRegistry.all] (plus a synthetic "none" sentinel
 * at the head), so adding a new provider auto-populates the dropdown without
 * touching this file.
 */
object AiProviderSetting : DropDownSettingsItem() {
    override val id = "ai_provider"
    override val displayName = "AI Provider"
    override val description = "Which AI provider the assistant uses. Select None to disable AI editing."
    override val group = SettingsGroup.AiAssistant
    override val defaultValue = NONE_ID

    override val options: List<DropDownOption> =
        listOf(DropDownOption(NONE_ID, "None")) +
            AiProviderRegistry.all().map { DropDownOption(it.id, it.displayName) }

    /** Sentinel id used when no provider is selected. */
    const val NONE_ID: String = "none"
}
