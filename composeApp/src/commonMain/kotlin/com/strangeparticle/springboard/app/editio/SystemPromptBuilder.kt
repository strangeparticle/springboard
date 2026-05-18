package com.strangeparticle.springboard.app.editio

import com.strangeparticle.springboard.app.editio.help.AiAssistantSystemPromptText

internal object SystemPromptBuilder {
    fun build(): String = AiAssistantSystemPromptText.text
}
