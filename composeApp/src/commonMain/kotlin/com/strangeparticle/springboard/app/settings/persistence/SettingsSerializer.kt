package com.strangeparticle.springboard.app.settings.persistence

import kotlinx.serialization.json.Json

/**
 * Shared serialization logic for user settings.
 * Converts between [UserSettingsDto] and JSON text.
 */
object SettingsSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Serializes a [UserSettingsDto] to a JSON string.
     */
    fun toJson(dto: UserSettingsDto): String {
        return json.encodeToString(UserSettingsDto.serializer(), dto)
    }

    /**
     * Deserializes a JSON string to a [UserSettingsDto].
     * Returns null if the JSON is invalid.
     */
    fun fromJson(jsonString: String): UserSettingsDto? {
        return try {
            json.decodeFromString(UserSettingsDto.serializer(), jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
