package com.strangeparticle.springboard.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Coordinate(
    @SerialName("environment_id")
    val environmentId: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("resource_id")
    val resourceId: String,
)
