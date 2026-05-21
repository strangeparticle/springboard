package com.strangeparticle.springboard.app.settings.persistence

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * One-shot adapter that reads the pre-refactor `settings.json` shape (typed
 * top-level fields like `aiOpenaiApiKey`, `activeBrand`) and re-emits the
 * contents as the new id-keyed [SettingsDto] map.
 *
 * Removal target: after one release cycle in production. Track removal by
 * searching for any reference to [LEGACY_FIELD_TO_NEW_ID].
 */
internal object SettingsDtoLegacyMigration {

    /**
     * Mapping from the old top-level JSON field name to the new [SettingsItem.id].
     * The `aiModel` field (previously a single string shared across providers) is
     * routed conditionally — see [migrate].
     */
    private val LEGACY_FIELD_TO_NEW_ID: Map<String, String> = mapOf(
        "startupTabs" to "startup_tabs",
        "openUrlsInNewWindowSingle" to "open_urls_in_new_window_single",
        "openUrlsInNewWindowMultiple" to "open_urls_in_new_window_multiple",
        "surfaceAppleScriptErrors" to "surface_applescript_errors",
        "resetKeyNavAfterKeyNavActivation" to "reset_key_nav_after_key_nav_activation",
        "resetKeyNavAfterGridNavActivation" to "reset_key_nav_after_grid_nav_activation",
        "activeBrand" to "active_brand",
        "aiProvider" to "ai_provider",
        "aiOpenaiApiKey" to "ai.openai.api_key",
        "aiAnthropicApiKey" to "ai.anthropic.api_key",
        "showFullChatTranscript" to "show_full_chat_transcript",
    )

    /**
     * If [root] looks like the legacy shape (no `values` field, but contains
     * one or more of the legacy field names), return a migrated map ready to
     * be wrapped in a [SettingsDto]. Otherwise return null — the caller falls
     * back to direct deserialization of the new shape.
     */
    fun migrate(root: JsonObject): Map<String, JsonElement>? {
        if ("values" in root) return null
        val legacyKeysPresent = LEGACY_FIELD_TO_NEW_ID.keys.any { it in root } || "aiModel" in root
        if (!legacyKeysPresent) return null

        val out = mutableMapOf<String, JsonElement>()
        for ((legacyField, newId) in LEGACY_FIELD_TO_NEW_ID) {
            val value = root[legacyField] ?: continue
            out[newId] = value
        }
        // Route the unified legacy `aiModel` to the right per-provider preferred-model id.
        val aiModel = root["aiModel"]
        val aiProvider = (root["aiProvider"] as? JsonPrimitive)?.jsonPrimitive?.content
        if (aiModel != null && aiProvider != null) {
            when (aiProvider) {
                "openai" -> out["ai.openai.preferred_model"] = aiModel
                "anthropic" -> out["ai.anthropic.preferred_model"] = aiModel
                else -> { /* unknown provider id: drop the model */ }
            }
        }
        return out
    }
}
