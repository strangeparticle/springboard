package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.SettingsItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

abstract class IntSettingsItem : SettingsItem<Int> {
    override val valueClass = Int::class

    open val minimumValue: Int = Int.MIN_VALUE
    open val maximumValue: Int = Int.MAX_VALUE

    override fun serialize(value: Int): JsonElement = JsonPrimitive(value)

    override fun deserialize(json: JsonElement): Int = validate(json.jsonPrimitive.int)

    override fun coerceFromString(raw: String): Int {
        val parsed = raw.toIntOrNull()
            ?: throw IllegalArgumentException("'$raw' is not a valid integer")
        return validate(parsed)
    }

    private fun validate(value: Int): Int {
        if (value < minimumValue || value > maximumValue) {
            throw IllegalArgumentException("value must be between $minimumValue and $maximumValue")
        }
        return value
    }
}
