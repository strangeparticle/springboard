package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import com.strangeparticle.springboard.app.domain.model.Coordinate
import kotlinx.serialization.Serializable

@Serializable
internal data class ActivateCoordinatesToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to activate against.")
    val tab_id: String,
    @ToolFieldDescription("List of (environment_id, app_id, resource_id) coordinates to activate as a single batch. Mirrors shift-select activation within one tab.")
    val coordinates: List<Coordinate>,
)
