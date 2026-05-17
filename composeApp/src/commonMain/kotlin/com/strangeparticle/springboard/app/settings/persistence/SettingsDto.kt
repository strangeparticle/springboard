package com.strangeparticle.springboard.app.settings.persistence

import com.strangeparticle.springboard.app.settings.StringFromDropDown
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsValues
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    val startupTabs: List<String>? = null,
    val openUrlsInNewWindowSingle: Boolean? = null,
    val openUrlsInNewWindowMultiple: Boolean? = null,
    val surfaceAppleScriptErrors: Boolean? = null,
    val resetKeyNavAfterKeyNavActivation: Boolean? = null,
    val resetKeyNavAfterGridNavActivation: Boolean? = null,
    val activeBrand: String? = null,
    val aiProvider: String? = null,
    val aiOpenaiApiKey: String? = null,
    val aiAnthropicApiKey: String? = null,
    val aiModel: String? = null,
) {
    fun toSettingsValues(): SettingsValues {
        var values = SettingsValues()
        if (startupTabs != null) {
            values = values.withSetting(SettingsKey.STARTUP_TABS, startupTabs)
        }
        if (openUrlsInNewWindowSingle != null) {
            values = values.withSetting(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE, openUrlsInNewWindowSingle)
        }
        if (openUrlsInNewWindowMultiple != null) {
            values = values.withSetting(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE, openUrlsInNewWindowMultiple)
        }
        if (surfaceAppleScriptErrors != null) {
            values = values.withSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, surfaceAppleScriptErrors)
        }
        if (resetKeyNavAfterKeyNavActivation != null) {
            values = values.withSetting(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION, resetKeyNavAfterKeyNavActivation)
        }
        if (resetKeyNavAfterGridNavActivation != null) {
            values = values.withSetting(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION, resetKeyNavAfterGridNavActivation)
        }
        if (activeBrand != null) {
            val declaration = SettingsRegistry.require(SettingsKey.ACTIVE_BRAND).defaultValue as StringFromDropDown
            if (declaration.isAllowed(activeBrand)) {
                values = values.withSetting(SettingsKey.ACTIVE_BRAND, activeBrand)
            }
        }
        if (aiProvider != null) {
            val declaration = SettingsRegistry.require(SettingsKey.AI_PROVIDER).defaultValue as StringFromDropDown
            if (declaration.isAllowed(aiProvider)) {
                values = values.withSetting(SettingsKey.AI_PROVIDER, aiProvider)
            }
        }
        if (aiOpenaiApiKey != null) {
            values = values.withSetting(SettingsKey.AI_OPENAI_API_KEY, aiOpenaiApiKey)
        }
        if (aiAnthropicApiKey != null) {
            values = values.withSetting(SettingsKey.AI_ANTHROPIC_API_KEY, aiAnthropicApiKey)
        }
        if (aiModel != null) {
            values = values.withSetting(SettingsKey.AI_MODEL, aiModel)
        }
        return values
    }

    companion object {
        fun fromSettingsValues(values: SettingsValues): SettingsDto {
            return SettingsDto(
                startupTabs = values.startupTabs,
                openUrlsInNewWindowSingle = values.openUrlsInNewWindowSingle,
                openUrlsInNewWindowMultiple = values.openUrlsInNewWindowMultiple,
                surfaceAppleScriptErrors = values.surfaceApplescriptErrors,
                resetKeyNavAfterKeyNavActivation = values.resetKeyNavAfterKeyNavActivation,
                resetKeyNavAfterGridNavActivation = values.resetKeyNavAfterGridNavActivation,
                activeBrand = values.activeBrand,
                aiProvider = values.aiProvider,
                aiOpenaiApiKey = values.aiOpenaiApiKey,
                aiAnthropicApiKey = values.aiAnthropicApiKey,
                aiModel = values.aiModel,
            )
        }
    }
}
