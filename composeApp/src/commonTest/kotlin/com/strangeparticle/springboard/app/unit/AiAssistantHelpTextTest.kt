package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.editio.help.AiAssistantFullHelpText
import com.strangeparticle.springboard.app.editio.help.AiAssistantSystemPromptText
import com.strangeparticle.springboard.app.editio.help.AiAssistantTerseHelpText
import kotlin.test.Test
import kotlin.test.assertContains

internal class AiAssistantHelpTextTest {

    @Test
    fun `terse user-facing help is available from common help package`() {
        assertContains(AiAssistantTerseHelpText.text, "Describe the springboard change you desire")
        assertContains(AiAssistantTerseHelpText.text, "E.g.:")
        assertContains(AiAssistantTerseHelpText.text, "Reference apps")
    }

    @Test
    fun `full user-facing help is available from common help package`() {
        assertContains(AiAssistantFullHelpText.title, "AI Editing Assistant help")
        assertContains(AiAssistantFullHelpText.text, AiAssistantFullHelpText.title)
        assertContains(AiAssistantFullHelpText.text, "Common requests:")
    }

    @Test
    fun `system prompt text is available from common help package`() {
        assertContains(AiAssistantSystemPromptText.text, "You are the AI Editing Assistant")
        assertContains(AiAssistantSystemPromptText.text, "Editing rules:")
        assertContains(AiAssistantSystemPromptText.text, "Communication rules:")
    }
}
