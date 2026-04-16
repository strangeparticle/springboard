package com.strangeparticle.springboard.app.persistence

import kotlinx.serialization.Serializable

@Serializable
data class TabDto(
    val tabId: String,
    val source: String? = null,
    val zoomPercent: Int? = null,
)
