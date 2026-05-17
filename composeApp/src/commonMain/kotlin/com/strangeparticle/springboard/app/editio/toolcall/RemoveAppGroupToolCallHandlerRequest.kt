package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class RemoveAppGroupToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Id of the appgroup to remove.")
    val id: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
