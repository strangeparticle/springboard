package com.strangeparticle.editio.client.provider.openai.response

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiResponseToolCallDto(
    val id: String,
    val type: String? = null,
    val function: com.strangeparticle.editio.client.provider.openai.response.OpenAiResponseToolCallFunctionDto,
)
