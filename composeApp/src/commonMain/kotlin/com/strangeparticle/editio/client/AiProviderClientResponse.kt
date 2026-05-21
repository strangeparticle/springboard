package com.strangeparticle.editio.client

import com.strangeparticle.editio.toolcall.ToolCall
import kotlinx.serialization.json.JsonObject

/** The single full provider-neutral response from one model turn. */
internal data class AiProviderClientResponse(
    val text: String?,
    val toolCalls: List<ToolCall>,
    val stopReason: AiProviderClientStopReason,
    val raw: JsonObject,
)
