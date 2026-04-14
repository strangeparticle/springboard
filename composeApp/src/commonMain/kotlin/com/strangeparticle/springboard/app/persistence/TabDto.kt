package com.strangeparticle.springboard.app.persistence

import kotlinx.serialization.Serializable

@Serializable
data class TabDto(
    val tabId: String,
    val sourceFilename: String? = null,
    val gridZoomSelection: String? = null,
)
