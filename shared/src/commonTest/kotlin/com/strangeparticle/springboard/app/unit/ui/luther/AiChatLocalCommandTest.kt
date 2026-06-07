package com.strangeparticle.springboard.app.unit.ui.luther

import com.strangeparticle.springboard.app.ui.luther.AiChatLocalCommand
import com.strangeparticle.springboard.app.ui.luther.parseAiChatLocalCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AiChatLocalCommandTest {
    @Test
    fun `parse recognizes terse help command`() {
        assertEquals(AiChatLocalCommand.HelpTerse(originalText = "/help_terse"), parseAiChatLocalCommand("/help_terse"))
    }

    @Test
    fun `parse recognizes full help command`() {
        assertEquals(AiChatLocalCommand.HelpFull(originalText = "/help"), parseAiChatLocalCommand("/help"))
    }

    @Test
    fun `parse trims and ignores full help command case`() {
        assertEquals(AiChatLocalCommand.HelpFull(originalText = "/HELP"), parseAiChatLocalCommand("  /HELP  "))
    }

    @Test
    fun `parse recognizes full help aliases`() {
        assertEquals(AiChatLocalCommand.HelpFull(originalText = "/help_full"), parseAiChatLocalCommand("/help_full"))
        assertEquals(AiChatLocalCommand.HelpFull(originalText = "/help_verbose"), parseAiChatLocalCommand("/help_verbose"))
    }

    @Test
    fun `parse returns null for normal prompt`() {
        assertNull(parseAiChatLocalCommand("add a logs URL for fretnaut"))
    }

    @Test
    fun `parse recognizes unknown slash command`() {
        assertEquals(AiChatLocalCommand.Unknown(originalText = "/wat"), parseAiChatLocalCommand(" /wat "))
    }
}
