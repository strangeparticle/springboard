package com.strangeparticle.springboard.app.ui.editio

internal sealed class AiChatLocalCommand {
    abstract val originalText: String

    data class HelpTerse(override val originalText: String) : AiChatLocalCommand()
    data class HelpFull(override val originalText: String) : AiChatLocalCommand()
    data class Unknown(override val originalText: String) : AiChatLocalCommand()
}

internal fun parseAiChatLocalCommand(input: String): AiChatLocalCommand? {
    val text = input.trim()
    if (!text.startsWith("/")) return null
    return when (text.lowercase()) {
        "/help_terse" -> AiChatLocalCommand.HelpTerse(originalText = text)
        "/help", "/help_full", "/help_verbose" -> AiChatLocalCommand.HelpFull(originalText = text)
        else -> AiChatLocalCommand.Unknown(originalText = text)
    }
}
