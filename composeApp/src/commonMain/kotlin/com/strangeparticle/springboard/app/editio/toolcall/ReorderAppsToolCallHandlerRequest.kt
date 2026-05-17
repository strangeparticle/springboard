package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class ReorderAppsToolCallHandlerRequest(
    val tab_id: String,
    @ToolFieldDescription("Full new ordering.")
    val ordered_ids: List<String>,
    val display_message: String,
)
