package com.strangeparticle.editio.client.provider.anthropic.request

import kotlinx.serialization.Serializable

@Serializable
internal data class AnthropicToolChoiceDto(val type: String)
