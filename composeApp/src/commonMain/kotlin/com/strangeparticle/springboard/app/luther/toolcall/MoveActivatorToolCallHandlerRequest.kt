package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class MoveActivatorToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab containing the activator to move.")
    val from_tab_id: String,
    @ToolFieldDescription("Id of the tab to move the activator into.")
    val to_tab_id: String,
    @ToolFieldDescription("App id of the activator coordinate to move.")
    val app_id: String,
    @ToolFieldDescription("Resource id of the activator coordinate to move.")
    val resource_id: String,
    @ToolFieldDescription("Environment id of the activator coordinate to move.")
    val environment_id: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
