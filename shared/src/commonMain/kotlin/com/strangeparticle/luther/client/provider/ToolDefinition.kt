package com.strangeparticle.luther.client.provider

import kotlinx.serialization.json.JsonObject

internal data class ToolDefinition(val name: String, val description: String, val schema: JsonObject)
