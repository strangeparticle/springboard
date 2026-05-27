package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class AddGuidanceToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("App id of the coordinate to attach guidance to.")
    val app_id: String,
    @ToolFieldDescription("Resource id of the coordinate to attach guidance to.")
    val resource_id: String,
    @ToolFieldDescription("Environment id of the coordinate to attach guidance to.")
    val environment_id: String,
    @ToolFieldDescription("Ordered list of guidance lines.")
    val guidance_lines: List<String>,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
