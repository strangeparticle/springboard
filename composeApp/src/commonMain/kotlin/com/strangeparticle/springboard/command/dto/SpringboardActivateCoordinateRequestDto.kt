package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardActivateCoordinateRequestDto(
    val requestId: String,
    val tabId: String? = null,
    val environmentId: String,
    val appId: String,
    val resourceId: String,
)
