package com.strangeparticle.springboard.app.editio.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.editio.toolcall.ToolFieldDescription

@Serializable
internal data class RespondWithMessageToolCallHandlerRequest(
    @ToolFieldDescription("The message to show the user.")
    val display_message: String,
)
