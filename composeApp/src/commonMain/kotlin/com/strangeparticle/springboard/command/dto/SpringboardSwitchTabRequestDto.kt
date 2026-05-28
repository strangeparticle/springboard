package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardSwitchTabRequestDto(
    val requestId: String? = null,
    val tabIndex: Int,
)
