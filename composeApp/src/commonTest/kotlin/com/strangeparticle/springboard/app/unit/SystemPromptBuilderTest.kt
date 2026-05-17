package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.editio.SystemPromptBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

internal class SystemPromptBuilderTest {

    @Test
    fun `prompt explains Springboard entities and reserved ALL environment`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "apps")
        assertContains(prompt, "resources")
        assertContains(prompt, "environments")
        assertContains(prompt, "app groups")
        assertContains(prompt, "activators")
        assertContains(prompt, "guidance")
        assertContains(prompt, "ALL")
    }

    @Test
    fun `prompt says to act directly through tools and reserve respond tool for prose answers`() {
        val prompt = SystemPromptBuilder.build()

        assertContains(prompt, "Act directly")
        assertContains(prompt, "tools")
        assertContains(prompt, "respond_with_message")
        assertContains(prompt, "prose-only")
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
