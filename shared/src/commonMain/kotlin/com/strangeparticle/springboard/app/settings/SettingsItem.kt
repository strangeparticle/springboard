package com.strangeparticle.springboard.app.settings

import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

/**
 * One object per setting. Carries the schema and serialization codec — everything
 * the framework needs to identify, render, persist, and resolve the setting.
 *
 * The item is stateless: it does not hold the current value. State lives in
 * [SettingsValues] (layered, immutable), and resolved values flow through
 * [com.strangeparticle.springboard.app.settings.SettingsManager].
 *
 * Concrete settings extend one of the typed bases under [settings/items/base]
 * (e.g. [com.strangeparticle.springboard.app.settings.items.base.StringSettingsItem],
 * [com.strangeparticle.springboard.app.settings.items.base.BooleanSettingsItem],
 * [com.strangeparticle.springboard.app.settings.items.base.DropDownSettingsItem],
 * [com.strangeparticle.springboard.app.settings.items.base.ListOfStringSettingsItem],
 * [com.strangeparticle.springboard.app.settings.items.base.DropDownFromApiCallSettingsItem]),
 * which fill in [valueClass] + serialization.
 */
interface SettingsItem<T : Any> {
    /** Stable persistence id, e.g. "ai.openai.api_key". Used for the JSON map key
     *  and as the basis for the derived env-var name. */
    val id: String

    val displayName: String
    val description: String
    val group: SettingsGroup

    /** Environments where this setting applies. Defaults to all. */
    val applicability: Set<RuntimeEnvironment>
        get() = RuntimeEnvironment.entries.toSet()

    /**
     * Override the env-var name. Null = derive `SPRINGBOARD_<UPPER_ID>` via
     * [SettingsKeyNaming]. Plugins set this to a non-prefixed name like
     * `OPENAI_API_KEY` to preserve user-facing env-var semantics.
     */
    val envVarNameOverride: String?
        get() = null

    val defaultValue: T
    val valueClass: KClass<T>

    fun serialize(value: T): JsonElement
    fun deserialize(json: JsonElement): T

    /**
     * Coerce a raw string (from env var, CLI flag, URL param) to [T]. Throws
     * [IllegalArgumentException] if the string isn't a valid value for this item.
     */
    fun coerceFromString(raw: String): T
}
