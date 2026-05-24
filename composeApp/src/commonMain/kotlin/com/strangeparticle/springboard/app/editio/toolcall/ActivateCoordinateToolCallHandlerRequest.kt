package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class ActivateCoordinateToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to activate against.")
    val tab_id: String,
    @ToolFieldDescription("Environment id of the entry to activate.")
    val environment_id: String,
    @ToolFieldDescription("App id of the entry to activate.")
    val app_id: String,
    @ToolFieldDescription("Resource id of the entry to activate.")
    val resource_id: String,
)
