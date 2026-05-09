package com.strangeparticle.springboard.app.domain.factory

import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import com.strangeparticle.springboard.app.domain.model.Springboard
import kotlinx.serialization.json.Json

/**
 * Serializes a [Springboard] (domain) back to JSON. Inverse of
 * [SpringboardFactory.fromJson]: builds a [SpringboardDto] from the domain object via
 * [springboardToDto] and encodes it via kotlinx.serialization's standard pretty-print.
 *
 * Field order in the output follows the declared field order in [SpringboardDto].
 * Optional fields whose value matches their declared default (e.g. an app with no
 * `appGroupId`, an empty `guidanceData` list) are omitted from the output.
 */
object SpringboardJsonWriter {

    private val json: Json = Json {
        classDiscriminator = "type"
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun toJson(springboard: Springboard): String =
        json.encodeToString(SpringboardDto.serializer(), springboardToDto(springboard))
}
