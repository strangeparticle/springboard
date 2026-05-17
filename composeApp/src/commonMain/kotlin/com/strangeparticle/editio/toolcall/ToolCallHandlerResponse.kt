package com.strangeparticle.editio.toolcall

import kotlinx.serialization.json.Json

/** Marker for host-specific tool-call execution results. */
internal interface ToolCallHandlerResponse {
    fun toProviderMessageContent(json: Json = Json): String
}
