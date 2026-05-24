package com.strangeparticle.editio.session

import com.strangeparticle.editio.session.event.ChatHistoryItem
import com.strangeparticle.editio.session.event.StateSnapshotAddedChatHistoryItem

internal enum class ChatHistoryGroupType {
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
