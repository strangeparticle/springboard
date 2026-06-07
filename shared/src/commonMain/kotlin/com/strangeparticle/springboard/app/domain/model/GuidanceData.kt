package com.strangeparticle.springboard.app.domain.model

data class GuidanceData(
    val environmentId: String,
    val appId: String,
    val resourceId: String,
    val guidanceLines: List<String>
)
