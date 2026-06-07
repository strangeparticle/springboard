package com.strangeparticle.springboard.app.persistence

import kotlinx.serialization.Serializable

@Serializable
data class TabsDto(
    val tabs: List<TabDto> = emptyList(),
    val activeTabId: String? = null,
)
