package com.strangeparticle.springboard.app.luther

import com.strangeparticle.springboard.app.luther.help.AiAssistantSystemPromptText

internal object SystemPromptBuilder {
    fun build(): String = AiAssistantSystemPromptText.text
}
