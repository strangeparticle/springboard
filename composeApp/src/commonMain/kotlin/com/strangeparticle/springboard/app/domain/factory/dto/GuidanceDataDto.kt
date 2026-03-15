package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class GuidanceDataDto(
    val environmentId: String,
    val appId: String,
    val resourceId: String,
    val guidanceLines: List<String>
)
