package com.strangeparticle.editio.client.provider.openai.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class OpenAiToolFunctionDto(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)
