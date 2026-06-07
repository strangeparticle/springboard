package com.strangeparticle.luther.session

import com.strangeparticle.luther.session.event.toDebugDto
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatHistoryGroupDebugDto(
    val groupIndex: Int,
    val type: String,
    val items: List<com.strangeparticle.luther.session.event.ChatHistoryItemDebugDto>,
) {
    companion object {
        fun from(groupIndex: Int, group: ChatHistoryGroup): ChatHistoryGroupDebugDto = ChatHistoryGroupDebugDto(
            groupIndex = groupIndex,
            type = group.type.name,
            items = group.items.mapIndexed { itemIndex, item -> item.toDebugDto(itemIndex) },
        )
    }
}
