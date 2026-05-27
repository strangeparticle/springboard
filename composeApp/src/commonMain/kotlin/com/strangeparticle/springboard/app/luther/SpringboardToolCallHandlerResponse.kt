package com.strangeparticle.springboard.app.luther

import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Structured Springboard tool-call result returned to the model. */
@Serializable
internal data class SpringboardToolCallHandlerResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val state: SpringboardAppSnapshot? = null,
    @Transient override val endsTurn: Boolean = false,
    @Transient private val transcriptOutput: String? = null,
) : ToolCallHandlerResponse {
    override fun toProviderMessageContent(json: Json): String = json.encodeToString(toSerializableResult())
    override fun toTranscriptOutput(providerMessageContent: String): String = transcriptOutput ?: if (success && state != null) {
        "Applied."
    } else {
        providerMessageContent
    }

    private fun toSerializableResult(): SerializableResult = SerializableResult(
        success = success,
        message = message,
        code = code,
    )

    @Serializable
    private data class SerializableResult(
        val success: Boolean,
        val message: String? = null,
        val code: String? = null,
    )
}
