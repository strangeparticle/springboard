package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class DisplayHintsDto(
    val width: Int? = null,
    val height: Int? = null
)
