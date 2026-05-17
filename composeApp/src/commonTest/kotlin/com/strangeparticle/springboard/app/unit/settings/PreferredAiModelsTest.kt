package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.editio.client.AiClientModelInfo
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import com.strangeparticle.springboard.app.settings.ai.PreferredAiModels
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PreferredAiModelsTest {

    @Test
    fun `selectPreferred returns first preferred available model for provider`() {
        val models = listOf(
            AiClientModelInfo("gpt-4.1", "GPT 4.1", supportsToolCalling = true),
            AiClientModelInfo("gpt-5", "GPT 5", supportsToolCalling = true),
        )

        assertEquals("gpt-5", PreferredAiModels.selectPreferred(AiProvider.OpenAi, models)?.id)
    }

    @Test
    fun `selectPreferred falls back to first tool capable model`() {
        val models = listOf(
            AiClientModelInfo("unknown-text", "Unknown", supportsToolCalling = false),
            AiClientModelInfo("custom-tool-model", "Custom", supportsToolCalling = true),
        )

        assertEquals("custom-tool-model", PreferredAiModels.selectPreferred(AiProvider.OpenAi, models)?.id)
    }
}
