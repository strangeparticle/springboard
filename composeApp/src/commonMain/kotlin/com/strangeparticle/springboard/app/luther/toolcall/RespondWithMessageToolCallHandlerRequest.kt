package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.luther.toolcall.ToolFieldDescription

@Serializable
internal data class RespondWithMessageToolCallHandlerRequest(
    @ToolFieldDescription("The message to show the user.")
    val display_message: String,
)
