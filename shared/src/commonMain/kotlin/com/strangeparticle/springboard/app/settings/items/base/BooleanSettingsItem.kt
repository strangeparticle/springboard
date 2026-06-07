package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.SettingsItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

abstract class BooleanSettingsItem : SettingsItem<Boolean> {
    override val valueClass = Boolean::class
    override fun serialize(value: Boolean): JsonElement = JsonPrimitive(value)
    override fun deserialize(json: JsonElement): Boolean = json.jsonPrimitive.boolean
    override fun coerceFromString(raw: String): Boolean =
        raw.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("'$raw' is not a valid boolean")
}
