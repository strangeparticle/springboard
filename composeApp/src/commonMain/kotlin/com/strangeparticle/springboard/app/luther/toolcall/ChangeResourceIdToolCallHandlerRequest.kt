package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class ChangeResourceIdToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Current id of the resource whose id should change.")
    val id: String,
    @ToolFieldDescription("Replacement id for the existing resource.")
    val new_id: String,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
