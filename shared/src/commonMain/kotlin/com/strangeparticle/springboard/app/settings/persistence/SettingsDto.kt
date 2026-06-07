package com.strangeparticle.springboard.app.settings.persistence

import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsValues
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * On-disk persistence envelope. The map is keyed by [SettingsItem.id]; values
 * are stored as [JsonElement] (each item owns its [SettingsItem.serialize] /
 * [SettingsItem.deserialize] codec). `JsonElement` only appears at this
 * persistence boundary — application-layer code sees typed values via
 * [SettingsValues.get] / [SettingsManager.resolveValue].
 *
 * The legacy typed-fields shape from earlier releases is migrated forward by
 * [SettingsDtoLegacyMigration] during read.
 */
@Serializable
data class SettingsDto(val values: Map<String, JsonElement> = emptyMap()) {

    /**
     * Convert this DTO back into a [SettingsValues] layer, looking up each id
     * in [registry] and delegating to the item's [SettingsItem.deserialize].
     * Unknown ids are skipped silently (forward compatibility — a setting
     * declared in a newer build is harmless to ignore in an older one).
     */
    fun toSettingsValues(registry: SettingsRegistry): SettingsValues {
        var result = SettingsValues()
        for ((id, json) in values) {
            val item = registry.byId(id) ?: continue
            @Suppress("UNCHECKED_CAST")
            val typedItem = item as SettingsItem<Any>
            try {
                val value = typedItem.deserialize(json)
                result = result.withRawSetting(id, value)
            } catch (_: Exception) {
                // Malformed value for an otherwise-known id: skip.
            }
        }
        return result
    }

    companion object {
        /**
         * Build a DTO from a [SettingsValues] layer. Only emits ids that the
         * registry knows about and that have a non-null value in the layer.
         */
        fun fromSettingsValues(values: SettingsValues, registry: SettingsRegistry): SettingsDto {
            val out = mutableMapOf<String, JsonElement>()
            for (item in registry.all()) {
                @Suppress("UNCHECKED_CAST")
                val typedItem = item as SettingsItem<Any>
                val value = values.get(typedItem) ?: continue
                out[item.id] = typedItem.serialize(value)
            }
            return SettingsDto(out)
        }
    }
}
