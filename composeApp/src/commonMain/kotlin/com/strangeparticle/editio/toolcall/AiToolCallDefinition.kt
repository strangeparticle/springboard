package com.strangeparticle.editio.toolcall

import kotlinx.serialization.json.JsonObject

/**
 * Description of a tool the model may call on a request. [schema] is a JSON Schema
 * object describing the tool's input arguments.
 */
internal data class AiToolCallDefinition(
    val name: String,
    val description: String,
    val schema: JsonObject,
)
