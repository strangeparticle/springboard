package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenFromUrlToolCallHandlerRequest(
    @ToolFieldDescription("The URL to load the springboard from (http, https, or s3).")
    val url: String,
    @ToolFieldDescription("When true, open in a new tab instead of replacing the current tab.")
    val in_new_tab: Boolean = false,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
