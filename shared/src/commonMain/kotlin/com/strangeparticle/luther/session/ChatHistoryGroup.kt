package com.strangeparticle.luther.session

import com.strangeparticle.luther.session.event.ChatHistoryItem
import com.strangeparticle.luther.session.event.StateSnapshotAddedChatHistoryItem

internal enum class ChatHistoryGroupType {
    // Currently only holds provider/model change entries, so it's named for that one concrete
    // item. If other session-state-like history items appear (e.g. system prompt or temperature
    // changes), this is the natural place to generalize the group into something broader like
    // SESSION_STATE. Kept specific until that second case actually exists.
    PROVIDER_MODEL_CHANGE,
    AI_INTERACTION,
    LOCAL_COMMAND,
}

internal data class ChatHistoryGroup(
    val type: ChatHistoryGroupType,
    val items: List<ChatHistoryItem>,
) {
    val preSnapshotJson: String?
        get() = (items.firstOrNull() as? StateSnapshotAddedChatHistoryItem)?.snapshotJson
}
