package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class ActivateRowToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to activate against.")
    val tab_id: String,
    @ToolFieldDescription("Environment id for the row's section. Use the all-envs id to activate from the all-envs section.")
    val environment_id: String,
    @ToolFieldDescription("Resource id whose row should be activated. Activates every app that has an activator for this resource (with all-envs fallback).")
    val resource_id: String,
)
