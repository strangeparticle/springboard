package com.strangeparticle.springboard.app.settings.ai

/**
 * Identifies which AI provider implementation the user has selected.
 * Stored as the option id of the `AI_PROVIDER` settings dropdown.
 */
enum class AiProvider(val id: String, val displayName: String) {
    None("none", "None"),
    OpenAi("openai", "OpenAI"),
    Anthropic("anthropic", "Anthropic"),
    ;

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: None
    }
}
