package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class OpenLocalFileToolCallHandlerRequest(
    val path: String,
    @ToolFieldDescription("When true, open in a new tab.")
    val in_new_tab: Boolean = false,
    val display_message: String,
)
