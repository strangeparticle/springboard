package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardShowGuidanceRequestDto(
    val requestId: String? = null,
    val environmentId: String,
    val appId: String,
    val resourceId: String,
)
