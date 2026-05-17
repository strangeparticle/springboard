package com.strangeparticle.editio.client.provider.openai.error

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiErrorResponseDto(
    val error: com.strangeparticle.editio.client.provider.openai.error.OpenAiErrorDto,
)
