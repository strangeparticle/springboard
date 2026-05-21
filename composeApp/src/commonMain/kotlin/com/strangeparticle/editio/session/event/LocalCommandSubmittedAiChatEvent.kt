package com.strangeparticle.editio.session.event

internal data class LocalCommandSubmittedAiChatEvent(
    val commandText: String,
    val source: LocalCommandSource,
) : AiChatEvent
