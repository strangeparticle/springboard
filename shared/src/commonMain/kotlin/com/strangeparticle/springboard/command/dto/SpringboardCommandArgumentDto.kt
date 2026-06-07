package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpringboardCommandArgumentDto(
    val id: String,
    val toolCallName: String,
    val description: String,
    val required: Boolean,
    val valueType: String,
)
