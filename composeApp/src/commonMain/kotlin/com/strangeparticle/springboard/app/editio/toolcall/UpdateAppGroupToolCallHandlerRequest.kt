package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class UpdateAppGroupToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Id of the appgroup to update.")
    val id: String,
    @ToolFieldDescription("New description for the appgroup. Omit to leave the current value unchanged.")
    val description: String? = null,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
