package com.strangeparticle.springboard.app.settings

import kotlin.reflect.KClass

/**
 * Metadata for a single setting. Sealed so that exhaustive `when` checks
 * cover all subclasses without an `else` branch.
 *
 * The [type] KClass acts as the discriminator that drives both value
 * coercion (env/CLI parsing) and UI rendering. For example, settings with
 * `type = StringFromDropDown::class` are rendered as dropdowns; settings with
 * `type = Boolean::class` are rendered as switches.
 *
 * External names (env var, CLI flag, URL param) are derived from the [key]
 * by [SettingsKeyNaming] — not stored here.
 */
sealed class SettingItem(
    val key: SettingsKey,
    val type: KClass<*>,
    val defaultValue: Any?,
    val displayName: String,
    val description: String,
) {
    /** A setting that applies to all runtime environments. */
    class General(
        key: SettingsKey,
        type: KClass<*>,
        defaultValue: Any?,
        displayName: String,
        description: String,
    ) : SettingItem(key, type, defaultValue, displayName, description)

    /** A setting that applies only to specific desktop runtime environments. */
    class Desktop(
        key: SettingsKey,
        type: KClass<*>,
        defaultValue: Any?,
        displayName: String,
        description: String,
        val runtimeEnvironments: List<RuntimeEnvironment>,
    ) : SettingItem(key, type, defaultValue, displayName, description)
}
