package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardCommandDefinitionDto(
    val id: String,
    val toolCallName: String,
    val summary: String,
    val arguments: List<SpringboardCommandArgumentDto>,
)
