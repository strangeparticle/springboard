package com.strangeparticle.springboard.app.ai.core

import kotlinx.serialization.json.Json

/**
 * Serializer for [AppStateSnapshot]. Encodes to a compact form (no whitespace, no
 * newlines anywhere) — that's the form fed into model-facing payloads (tool results,
 * snapshot injections in conversation history). Decoding accepts both compact and
 * pretty forms so tests can hand-write fixtures.
 *
 * Per spec §3.2.
 */
internal object AppStateSnapshotJson {

    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = false
    }

    fun toCompactJson(snapshot: AppStateSnapshot): String =
        json.encodeToString(AppStateSnapshot.serializer(), snapshot)

    fun fromJson(jsonString: String): AppStateSnapshot =
        json.decodeFromString(AppStateSnapshot.serializer(), jsonString)
}
