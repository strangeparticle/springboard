package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateTabToolCallHandlerRequest(
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
