package com.strangeparticle.luther.session

import com.strangeparticle.luther.session.event.ChatHistoryItem
import com.strangeparticle.luther.session.event.StateSnapshotAddedChatHistoryItem

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
