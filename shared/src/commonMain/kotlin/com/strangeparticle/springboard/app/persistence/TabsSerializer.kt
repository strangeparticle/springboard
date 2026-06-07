package com.strangeparticle.springboard.app.persistence

import kotlinx.serialization.json.Json

object TabsSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun toJson(dto: TabsDto): String {
        return json.encodeToString(TabsDto.serializer(), dto)
    }

    fun fromJson(jsonString: String): TabsDto {
        return try {
            json.decodeFromString(TabsDto.serializer(), jsonString)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Invalid tabs JSON", exception)
        }
    }
}
