package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class AddUrlActivatorToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("App id for the activator coordinate.")
    val app_id: String,
    @ToolFieldDescription("Resource id for the activator coordinate.")
    val resource_id: String,
    @ToolFieldDescription("Environment id for the activator coordinate.")
    val environment_id: String,
    @ToolFieldDescription("The full URL to open when this activator is triggered.")
    val url: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
