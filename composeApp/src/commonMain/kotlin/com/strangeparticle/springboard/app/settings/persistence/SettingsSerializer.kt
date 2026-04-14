package com.strangeparticle.springboard.app.settings.persistence

import kotlinx.serialization.json.Json

/**
 * Shared serialization logic for user settings.
 * Converts between [SettingsDto] and JSON text.
 */
object SettingsSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Serializes a [SettingsDto] to a JSON string.
     */
    fun toJson(dto: SettingsDto): String {
        return json.encodeToString(SettingsDto.serializer(), dto)
    }

    /**
     * Deserializes a JSON string to a [SettingsDto].
     * Throws [IllegalArgumentException] if the JSON is invalid.
     */
    fun fromJson(jsonString: String): SettingsDto {
        return try {
            json.decodeFromString(SettingsDto.serializer(), jsonString)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Invalid settings JSON", exception)
        }
    }
}
