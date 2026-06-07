package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.runAppleScriptFile
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopAppleScriptRunnerTest {

    @Test
    fun `runAppleScriptFile with args forwards them to osascript`() {
        val result = runAppleScriptFile(
            "applescript/echo_argv.applescript",
            listOf("first", "second", "third"),
        )

        assertEquals(0, result.exitCode, "stderr was: ${result.stderr}")
        assertEquals("first|second|third", result.stdout)
    }

    @Test
    fun `runAppleScriptFile with empty args behaves like the argless overload`() {
        val withEmptyArgs = runAppleScriptFile(
            "applescript/echo_argv.applescript",
            emptyList(),
        )
        val argless = runAppleScriptFile("applescript/echo_argv.applescript")

        assertEquals(0, withEmptyArgs.exitCode, "stderr was: ${withEmptyArgs.stderr}")
        assertEquals(0, argless.exitCode, "stderr was: ${argless.stderr}")
        assertEquals(argless.stdout, withEmptyArgs.stdout)
    }
}
