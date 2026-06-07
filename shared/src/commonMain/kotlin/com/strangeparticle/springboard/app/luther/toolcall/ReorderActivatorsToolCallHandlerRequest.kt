package com.strangeparticle.springboard.app.luther.toolcall

import kotlinx.serialization.Serializable
import com.strangeparticle.springboard.app.domain.model.Coordinate

@Serializable
internal data class ReorderActivatorsToolCallHandlerRequest(
    val tab_id: String,
    val ordered_coordinates: List<Coordinate>,
    val display_message: String,
)
