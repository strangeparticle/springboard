package com.strangeparticle.editio.client.provider.anthropic

import com.strangeparticle.editio.client.AiClientModelInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Filters and maps Anthropic's `/v1/models` response down to the chat-capable subset
 * suitable for a model dropdown. Anthropic's model list includes only Claude models,
 * so the filter is straightforward: keep entries where `type == "model"` and `id`
 * starts with `"claude-"`. Sort descending by id so newer models surface first.
 */
internal object AnthropicModelFilter {

    fun filterAndMap(responseBody: JsonObject): List<AiClientModelInfo> {
        val data = responseBody["data"] as? JsonArray ?: return emptyList()
        return data
            .mapNotNull { entry ->
                val obj = entry as? JsonObject ?: return@mapNotNull null
                val type = (obj["type"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                if (type != "model") return@mapNotNull null
                val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                if (!id.startsWith("claude-")) return@mapNotNull null
                val displayName = (obj["display_name"] as? JsonPrimitive)?.contentOrNull ?: id
                AiClientModelInfo(id = id, displayName = displayName, supportsToolCalling = true)
            }
            .sortedByDescending { it.id }
    }
}
