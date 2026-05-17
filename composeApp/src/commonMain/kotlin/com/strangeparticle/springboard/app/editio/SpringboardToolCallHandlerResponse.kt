package com.strangeparticle.springboard.app.editio

import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Structured Springboard tool-call result returned to the model. */
@Serializable
internal data class SpringboardToolCallHandlerResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val state: SpringboardAppSnapshot? = null,
) : ToolCallHandlerResponse {
    override fun toProviderMessageContent(json: Json): String = json.encodeToString(this)
}
