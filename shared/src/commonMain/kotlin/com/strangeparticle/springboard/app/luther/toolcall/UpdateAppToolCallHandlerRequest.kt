package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class UpdateAppToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Id of the app to update.")
    val id: String,
    @ToolFieldDescription("New display name for the app.")
    val name: String? = null,
    @ToolFieldDescription("Optional replacement app group id.")
    val app_group_id: String? = null,
    @ToolFieldDescription("Set true to clear the current app group assignment.")
    val clear_app_group_id: Boolean = false,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
