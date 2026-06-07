package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class AddResourceToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Stable id for the new resource.")
    val id: String,
    @ToolFieldDescription("Display name for the new resource.")
    val name: String,

    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
