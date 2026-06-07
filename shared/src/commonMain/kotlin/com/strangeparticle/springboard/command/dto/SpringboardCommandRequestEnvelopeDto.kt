package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardCommandRequestEnvelopeDto(
    val protocolVersion: Int = 1,
    val requestId: String,
    val command: SpringboardCommandDto,
)
