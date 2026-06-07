package com.strangeparticle.springboard.app.settings.persistence

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Shared serialization logic for user settings.
 * Converts between [SettingsDto] and JSON text, with one-shot migration
 * from the legacy typed-fields shape (see [SettingsDtoLegacyMigration]).
 */
object SettingsSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun toJson(dto: SettingsDto): String =
        json.encodeToString(SettingsDto.serializer(), dto)

    /**
     * Deserialize a JSON settings file. Recognises both the new key/value
     * shape and the legacy typed-fields shape (migrated forward by
     * [SettingsDtoLegacyMigration]).
     *
     * Throws [IllegalArgumentException] if the JSON is invalid.
     */
    fun fromJson(jsonString: String): SettingsDto {
        return try {
            val root = json.parseToJsonElement(jsonString) as? JsonObject
                ?: throw IllegalArgumentException("Settings JSON must be a JSON object")
            val migrated = SettingsDtoLegacyMigration.migrate(root)
            if (migrated != null) {
                SettingsDto(values = migrated)
            } else {
                json.decodeFromString(SettingsDto.serializer(), jsonString)
            }
        } catch (exception: Exception) {
            throw IllegalArgumentException("Invalid settings JSON", exception)
        }
    }
}
