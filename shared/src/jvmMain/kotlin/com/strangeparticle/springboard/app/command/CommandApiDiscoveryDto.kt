package com.strangeparticle.springboard.app.command

import kotlinx.serialization.Serializable

@Serializable
data class CommandApiDiscoveryDto(
    val protocolVersion: Int = 1,
    val baseUrl: String,
    val token: String,
    val pid: Long,
    val startedAt: String,
)
