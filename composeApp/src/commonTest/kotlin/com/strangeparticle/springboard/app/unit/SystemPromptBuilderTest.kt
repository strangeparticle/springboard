package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.editio.SystemPromptBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

internal class SystemPromptBuilderTest {

    @Test
    fun `prompt explains Springboard entities and reserved ALL environment`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "AI Editing Assistant")
        assertContains(prompt, "tabs")
        assertContains(prompt, "tab_id")
        assertContains(prompt, "active tab")
        assertContains(prompt, "springboard")
        assertContains(prompt, "apps")
        assertContains(prompt, "resources")
        assertContains(prompt, "environments")
        assertContains(prompt, "app groups")
        assertContains(prompt, "activators")
        assertContains(prompt, "guidance")
        assertContains(prompt, "ALL")
    }

    @Test
    fun `prompt explains automatic id generation for new entities`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "When the user provides a name or description but no explicit id")
        assertContains(prompt, "generate")
        assertContains(prompt, "lowercase snake_case")
        assertContains(prompt, "Do not invent ids for existing entities")
        assertContains(prompt, "Productivity Tools")
        assertContains(prompt, "productivity_tools")
        assertContains(prompt, "Springboard General 1")
        assertContains(prompt, "springboard_general_1")
    }

    @Test
    fun `prompt says to act directly through tools and reserve respond tool for prose answers`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "Act directly")
        assertContains(prompt, "tools")
        assertContains(prompt, "respond_with_message")
        assertContains(prompt, "final prose answers")
    }

    @Test
    fun `prompt tells assistant not to imply success after provider or tool errors`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "rate-limit")
        assertContains(prompt, "report it as an error")
        assertContains(prompt, "do not imply the overall edit is complete")
    }

    @Test
    fun `prompt says only save springboard requires confirmation`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "Only save_springboard requires user confirmation")
    }

    @Test
    fun `prompt does not inline current app snapshot`() {
        val prompt = SystemPromptBuilder.build()

        assertFalse(prompt.contains("SpringboardAppSnapshot"))
        assertFalse(prompt.contains("<current_state>"))
    }
}
