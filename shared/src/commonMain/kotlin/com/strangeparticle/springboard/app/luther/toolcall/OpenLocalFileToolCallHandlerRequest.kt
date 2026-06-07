package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class OpenLocalFileToolCallHandlerRequest(
    val path: String,
    @ToolFieldDescription("When true, open in a new tab.")
    val in_new_tab: Boolean = false,
    val display_message: String,
)
