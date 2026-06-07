package com.strangeparticle.springboard.command

import com.strangeparticle.springboard.command.dto.SpringboardCommandDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandRequestEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResponseEnvelopeDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SpringboardCommandJson {
    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val envelopeJson: Json = Json(json) {
        encodeDefaults = true
        explicitNulls = true
    }

    fun encodeCommand(command: SpringboardCommandDto): String = json.encodeToString(command)

    fun decodeCommand(value: String): SpringboardCommandDto = json.decodeFromString(value)

    fun encodeRequest(request: SpringboardCommandRequestEnvelopeDto): String = envelopeJson.encodeToString(request)

    fun decodeRequest(value: String): SpringboardCommandRequestEnvelopeDto = envelopeJson.decodeFromString(value)

    fun encodeResponse(response: SpringboardCommandResponseEnvelopeDto): String = envelopeJson.encodeToString(response)

    fun decodeResponse(value: String): SpringboardCommandResponseEnvelopeDto = envelopeJson.decodeFromString(value)
}
