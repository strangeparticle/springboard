package com.strangeparticle.springboard.command

import kotlinx.serialization.json.JsonElement

sealed interface SpringboardCommandResult {
    data class Success(
        val message: String? = null,
        val data: JsonElement? = null,
    ) : SpringboardCommandResult

    data class Failure(
        val code: SpringboardCommandErrorCode,
        val message: String,
        val details: JsonElement? = null,
    ) : SpringboardCommandResult
}
