package com.strangeparticle.editio.client.provider.openai.request

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiToolDto(
    val type: String,
    val function: com.strangeparticle.editio.client.provider.openai.request.OpenAiToolFunctionDto,
)
