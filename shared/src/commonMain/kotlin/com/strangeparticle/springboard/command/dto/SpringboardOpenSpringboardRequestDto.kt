package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardOpenSpringboardRequestDto(
    val requestId: String? = null,
    val source: String,
    val inNewTab: Boolean = false,
)
