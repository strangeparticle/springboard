package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateSpringboardToolCallHandlerRequest(
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
