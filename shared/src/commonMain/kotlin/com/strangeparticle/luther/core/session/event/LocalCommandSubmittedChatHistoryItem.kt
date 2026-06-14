package com.strangeparticle.luther.core.session.event

internal data class LocalCommandSubmittedChatHistoryItem(
    val commandText: String,
    val source: LocalCommandSource,
) : ChatHistoryItem
