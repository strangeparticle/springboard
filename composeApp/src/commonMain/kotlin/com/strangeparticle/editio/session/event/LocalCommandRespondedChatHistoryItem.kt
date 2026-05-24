package com.strangeparticle.editio.session.event

internal data class LocalCommandRespondedChatHistoryItem(
    val commandText: String,
    val responseText: String,
    val kind: LocalCommandResponseKind,
) : ChatHistoryItem
