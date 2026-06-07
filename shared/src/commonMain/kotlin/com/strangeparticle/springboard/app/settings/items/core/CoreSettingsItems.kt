package com.strangeparticle.springboard.app.settings.items.core

import com.strangeparticle.springboard.app.settings.SettingsItem

/**
 * Flat list of every core (non-provider) [SettingsItem]. The framework
 * `SettingsRegistry` is assembled at startup by concatenating this with each
 * registered AI provider's `settingsItems()`.
 *
 * Adding a core setting: create its file under `settings/items/core/` and
 * append its singleton object to this list.
 */
fun coreSettingsItems(): List<SettingsItem<*>> = listOf(
    StartupTabsSetting,
    OpenUrlsInNewWindowSingleSetting,
    OpenUrlsInNewWindowMultipleSetting,
    SurfaceAppleScriptErrorsSetting,
    ResetKeyNavAfterKeyNavActivationSetting,
    ResetKeyNavAfterGridNavActivationSetting,
    ActiveBrandSetting,
    HideAppAfterActivationSetting,
    PreferredTerminalSetting,
    OpenTerminalInNewWindowSetting,
    HttpContentTimeoutSecondsSetting,
    AiProviderSetting,
    HttpAiProviderTimeoutSecondsSetting,
    ShowFullChatTranscriptSetting,
    OpenFromS3AwsProfileSetting,
)
