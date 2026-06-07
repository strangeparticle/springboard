package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.SettingsItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

abstract class ListOfStringSettingsItem : SettingsItem<List<String>> {
    @Suppress("UNCHECKED_CAST")
    override val valueClass: KClass<List<String>> = List::class as KClass<List<String>>

    override fun serialize(value: List<String>): JsonElement =
        JsonArray(value.map { JsonPrimitive(it) })

    override fun deserialize(json: JsonElement): List<String> =
        json.jsonArray.map { it.jsonPrimitive.content }

    override fun coerceFromString(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
