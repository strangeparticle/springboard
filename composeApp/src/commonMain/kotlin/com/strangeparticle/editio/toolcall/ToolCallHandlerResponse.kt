package com.strangeparticle.editio.toolcall

import kotlinx.serialization.json.Json

/** Marker for host-specific tool-call execution results. */
internal interface ToolCallHandlerResponse {
    val endsTurn: Boolean
        get() = false

    fun toProviderMessageContent(json: Json = Json): String

    fun toTranscriptOutput(providerMessageContent: String): String = providerMessageContent
}
