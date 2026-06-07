package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SpringboardDto(
    val name: String,
    val appGroups: List<AppGroupDto> = emptyList(),
    val apps: List<AppDto>,
    val resources: List<ResourceDto>,
    val environments: List<EnvironmentDto>,
    val activators: List<ActivatorDto>,
    val guidanceData: List<GuidanceDataDto> = emptyList(),
)
