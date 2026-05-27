package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class ActivateColumnToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to activate against.")
    val tab_id: String,
    @ToolFieldDescription("Environment id for the column's section. Use the all-envs id to activate from the all-envs section.")
    val environment_id: String,
    @ToolFieldDescription("App id whose column should be activated. Activates every resource the app has an activator for (with all-envs fallback).")
    val app_id: String,
)
