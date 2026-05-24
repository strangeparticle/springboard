package com.strangeparticle.editio.session.event

internal data class LocalCommandSubmittedChatHistoryItem(
    val commandText: String,
    val source: LocalCommandSource,
) : ChatHistoryItem
