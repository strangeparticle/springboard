package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateSpringboardToolCallHandlerRequest(
    @ToolFieldDescription("Optional name for the new unsaved Springboard. Omit to use the next generated Untitled name.")
    val name: String? = null,

    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)
