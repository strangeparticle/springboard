package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed interface SpringboardCommandResultDto {
    @Serializable
    @SerialName("success")
    data class Success(
        val message: String? = null,
        val data: JsonElement? = null,
    ) : SpringboardCommandResultDto

    @Serializable
    @SerialName("failure")
    data class Failure(
        val code: String,
        val message: String,
        val details: JsonElement? = null,
    ) : SpringboardCommandResultDto
}
