package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class ChangeResourceNameToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Id of the resource whose display name should change.")
    val id: String,
    @ToolFieldDescription("New display name for the existing resource.")
    val name: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
