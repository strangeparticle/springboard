package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable

@Serializable
internal data class RemoveGuidanceToolCallHandlerRequest(
    val tab_id: String,
    val app_id: String,
    val resource_id: String,
    val environment_id: String,
    val display_message: String,
)
