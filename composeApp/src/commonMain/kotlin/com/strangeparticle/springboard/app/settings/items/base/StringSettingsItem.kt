package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.SettingsItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

abstract class StringSettingsItem : SettingsItem<String> {
    override val valueClass = String::class
    override fun serialize(value: String): JsonElement = JsonPrimitive(value)
    override fun deserialize(json: JsonElement): String = json.jsonPrimitive.content
    override fun coerceFromString(raw: String): String = raw

    /**
     * When true, the framework's text-field widget masks the displayed value
     * (with a "Show / Hide" toggle). Used for API keys and similar secrets.
     */
    open val isSensitive: Boolean = false
}
