package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class UpdateActivatorToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("App id for the activator coordinate.")
    val app_id: String,
    @ToolFieldDescription("Resource id for the activator coordinate.")
    val resource_id: String,
    @ToolFieldDescription("Environment id for the activator coordinate.")
    val environment_id: String,
    @ToolFieldDescription("New URL for a URL activator.")
    val url: String? = null,
    @ToolFieldDescription("New URL template for a URL-template activator.")
    val url_template: String? = null,
    @ToolFieldDescription("New command template for a command activator.")
    val command_template: String? = null,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
