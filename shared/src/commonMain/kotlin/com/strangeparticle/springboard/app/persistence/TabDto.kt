package com.strangeparticle.springboard.app.persistence

import kotlinx.serialization.Serializable

@Serializable
data class TabDto(
    val tabId: String,
    val source: String? = null,
    val zoomPercent: Int? = null,
    val s3AwsProfile: String? = null,
    val s3LastEtag: String? = null,
)
