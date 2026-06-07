package com.strangeparticle.luther.session.event

internal data class LocalCommandSubmittedChatHistoryItem(
    val commandText: String,
    val source: LocalCommandSource,
) : ChatHistoryItem
