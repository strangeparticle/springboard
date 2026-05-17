package com.strangeparticle.springboard.app.editio

import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import com.strangeparticle.springboard.app.domain.factory.springboardToDto
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A point-in-time picture of Springboard app state across all open tabs. Used as:
 *
 * 1. The `tool_result` payload returned to the model after every tool call.
 * 2. A synthetic snapshot message injected into conversation history when
 *    [com.strangeparticle.editio.core.AiSessionManager.stateChangedSinceLastSnapshotSent]
 *    is true.
 * 3. The initial state visible to the model when the chat first starts.
 */
@Serializable
internal data class SpringboardAppSnapshot(
    val tabs: List<SpringboardTabSnapshot>,
    val activeTabId: String?,
) {
    fun toCompactJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json: Json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            encodeDefaults = false
        }

        fun capture(viewModel: SpringboardViewModel): SpringboardAppSnapshot {
            val tabSnapshots = viewModel.tabs.map { tab ->
                SpringboardTabSnapshot(
                    tabId = tab.tabId,
                    label = tab.label,
                    source = tab.source,
                    isDirty = tab.isDirty,
                    springboard = tab.springboard?.let(::springboardToDto),
                )
            }
            return SpringboardAppSnapshot(
                tabs = tabSnapshots,
                activeTabId = viewModel.activeTabId.takeIf { activeId ->
                    viewModel.tabs.any { it.tabId == activeId }
                },
            )
        }

        fun fromJson(jsonString: String): SpringboardAppSnapshot =
            json.decodeFromString(serializer(), jsonString)
    }
}
