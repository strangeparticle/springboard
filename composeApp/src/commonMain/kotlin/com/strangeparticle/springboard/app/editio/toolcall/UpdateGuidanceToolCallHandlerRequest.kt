package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class UpdateGuidanceToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("App id of the coordinate whose guidance to update.")
    val app_id: String,
    @ToolFieldDescription("Resource id of the coordinate whose guidance to update.")
    val resource_id: String,
    @ToolFieldDescription("Environment id of the coordinate whose guidance to update.")
    val environment_id: String,
    @ToolFieldDescription("Ordered list of guidance lines.")
    val guidance_lines: List<String>,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
