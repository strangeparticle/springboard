package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SpringboardDto(
    val name: String,
    val environments: List<EnvironmentDto>,
    val apps: List<AppDto>,
    val resources: List<ResourceDto>,
    val activators: List<ActivatorDto>,
    val displayHints: DisplayHintsDto? = null,
    val guidanceData: List<GuidanceDataDto> = emptyList()
)
