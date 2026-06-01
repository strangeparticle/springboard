package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.buildTerminalActivatorCommandLine
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalActivatorCommandLineUtilTest {

    @Test
    fun `directory only produces a quoted cd`() {
        val line = buildTerminalActivatorCommandLine("/my/project/dir", null)
        assertEquals("cd '/my/project/dir'", line)
    }

    @Test
    fun `directory and command are joined with and`() {
        val line = buildTerminalActivatorCommandLine("/my/project/dir", "ls -la")
        assertEquals("cd '/my/project/dir' && ls -la", line)
    }

    @Test
    fun `directory with spaces stays quoted without extra escaping`() {
        val line = buildTerminalActivatorCommandLine("/Users/me/My Projects", null)
        assertEquals("cd '/Users/me/My Projects'", line)
    }

    @Test
    fun `single quotes in the directory are escaped for the shell`() {
        val line = buildTerminalActivatorCommandLine("/Users/me/it's mine", null)
        assertEquals("cd '/Users/me/it'\\''s mine'", line)
    }

    @Test
    fun `blank command is treated as no command`() {
        val line = buildTerminalActivatorCommandLine("/my/dir", "   ")
        assertEquals("cd '/my/dir'", line)
    }

    @Test
    fun `command is passed through verbatim`() {
        val line = buildTerminalActivatorCommandLine("/dir", "git status && echo done")
        assertEquals("cd '/dir' && git status && echo done", line)
    }
}
