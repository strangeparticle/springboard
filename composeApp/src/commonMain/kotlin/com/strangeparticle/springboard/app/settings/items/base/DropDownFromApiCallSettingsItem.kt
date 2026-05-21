package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.DropDownOption
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * A string-valued setting whose options come from a live service call. The
 * framework's row composable handles the entire async lifecycle (initial load,
 * loading spinner, error display, refresh button) for any item extending this.
 *
 * Implementations declare [loadOptions], which receives a [SettingsItemContext]
 * for HTTP access and sibling-setting reads (e.g. reading the related API key).
 */
abstract class DropDownFromApiCallSettingsItem : SettingsItem<String> {
    override val valueClass = String::class
    override fun serialize(value: String): JsonElement = JsonPrimitive(value)
    override fun deserialize(json: JsonElement): String = json.jsonPrimitive.content

    /** Options are loaded asynchronously, so we can't validate against them here. */
    override fun coerceFromString(raw: String): String = raw

    abstract suspend fun loadOptions(context: SettingsItemContext): Result<List<DropDownOption>>
}
