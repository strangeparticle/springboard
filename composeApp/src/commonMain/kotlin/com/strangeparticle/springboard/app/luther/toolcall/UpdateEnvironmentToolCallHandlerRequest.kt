package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class UpdateEnvironmentToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Id of the environment to update.")
    val id: String,
    @ToolFieldDescription("New name for the environment.")
    val name: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
