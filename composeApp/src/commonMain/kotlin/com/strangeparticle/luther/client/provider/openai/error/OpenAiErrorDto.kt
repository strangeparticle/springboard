package com.strangeparticle.luther.client.provider.openai.error

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiErrorDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
