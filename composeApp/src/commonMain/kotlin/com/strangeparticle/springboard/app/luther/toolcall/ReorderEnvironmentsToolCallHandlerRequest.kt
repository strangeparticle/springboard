package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class ReorderEnvironmentsToolCallHandlerRequest(
    val tab_id: String,
    @ToolFieldDescription("Full new ordering.")
    val ordered_ids: List<String>,
    val display_message: String,
)
