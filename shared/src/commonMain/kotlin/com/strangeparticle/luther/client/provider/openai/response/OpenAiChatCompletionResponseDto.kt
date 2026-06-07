package com.strangeparticle.luther.client.provider.openai.response

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiChatCompletionResponseDto(
    val choices: List<com.strangeparticle.luther.client.provider.openai.response.OpenAiChoiceDto>,
)
