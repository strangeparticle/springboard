package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class AddAppToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Stable id for the new app.")
    val id: String,
    @ToolFieldDescription("Display name for the new app.")
    val name: String,
    @ToolFieldDescription("Optional id of an existing app group to assign this app to.")
    val app_group_id: String? = null,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
