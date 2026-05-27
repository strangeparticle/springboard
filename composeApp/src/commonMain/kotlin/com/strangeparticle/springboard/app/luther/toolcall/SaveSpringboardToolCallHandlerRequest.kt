package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable

@Serializable
internal data class SaveSpringboardToolCallHandlerRequest(
    val tab_id: String,
    val display_message: String,
)
