package com.strangeparticle.editio.session.event

internal data class LocalCommandRespondedAiChatEvent(
    val commandText: String,
    val responseText: String,
    val kind: LocalCommandResponseKind,
) : AiChatEvent
