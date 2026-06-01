package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.acceptance.PlatformAppleScriptRunnerServiceInMemoryFake
import com.strangeparticle.springboard.app.acceptance.PlatformBrowserDetectionServiceInMemoryFake
import com.strangeparticle.springboard.app.acceptance.PlatformTerminalDetectionServiceInMemoryFake
import com.strangeparticle.springboard.app.platform.PlatformActivationServiceDesktopImpl
import com.strangeparticle.springboard.app.platform.PreferredTerminal
import com.strangeparticle.springboard.app.platform.ScriptRunResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformActivationServiceDesktopImplTerminalTest {

    private fun service(
        runner: PlatformAppleScriptRunnerServiceInMemoryFake = PlatformAppleScriptRunnerServiceInMemoryFake(),
        terminalDetection: PlatformTerminalDetectionServiceInMemoryFake = PlatformTerminalDetectionServiceInMemoryFake(),
        surfaceAppleScriptErrors: Boolean = false,
    ) = PlatformActivationServiceDesktopImpl(
        browserDetectionService = PlatformBrowserDetectionServiceInMemoryFake(),
        appleScriptRunnerService = runner,
        terminalDetectionService = terminalDetection,
        surfaceAppleScriptErrors = surfaceAppleScriptErrors,
    )

    @Test
    fun `Terminal app new window runs the terminal new-window script with the cd line`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        service(runner = runner).openTerminal(
            workingDirectory = "/my/dir",
            command = null,
            preferredTerminal = PreferredTerminal.TerminalApp,
            openInNewWindow = true,
            onError = {},
        )

        val invocation = runner.scriptInvocations.single()
        assertEquals("applescript/terminal_new_window.applescript", invocation.resourcePath)
        assertEquals(listOf("cd '/my/dir'"), invocation.args)
    }

    @Test
    fun `Terminal app default placement runs the terminal front-window script`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        service(runner = runner).openTerminal(
            workingDirectory = "/my/dir",
            command = "ls",
            preferredTerminal = PreferredTerminal.TerminalApp,
            openInNewWindow = false,
            onError = {},
        )

        val invocation = runner.scriptInvocations.single()
        assertEquals("applescript/terminal_front_window.applescript", invocation.resourcePath)
        assertEquals(listOf("cd '/my/dir' && ls"), invocation.args)
    }

    @Test
    fun `iTerm new window runs the iterm new-window script when installed`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        service(
            runner = runner,
            terminalDetection = PlatformTerminalDetectionServiceInMemoryFake(iTermInstalled = true),
        ).openTerminal(
            workingDirectory = "/dir",
            command = null,
            preferredTerminal = PreferredTerminal.ITerm,
            openInNewWindow = true,
            onError = {},
        )

        assertEquals("applescript/iterm_new_window.applescript", runner.scriptInvocations.single().resourcePath)
    }

    @Test
    fun `iTerm default placement runs the iterm default script when installed`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        service(
            runner = runner,
            terminalDetection = PlatformTerminalDetectionServiceInMemoryFake(iTermInstalled = true),
        ).openTerminal(
            workingDirectory = "/dir",
            command = null,
            preferredTerminal = PreferredTerminal.ITerm,
            openInNewWindow = false,
            onError = {},
        )

        assertEquals("applescript/iterm_front_window_new_tab.applescript", runner.scriptInvocations.single().resourcePath)
    }

    @Test
    fun `iTerm not installed falls back to Terminal app and reports it`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        val errors = mutableListOf<String>()
        service(
            runner = runner,
            terminalDetection = PlatformTerminalDetectionServiceInMemoryFake(iTermInstalled = false),
        ).openTerminal(
            workingDirectory = "/dir",
            command = null,
            preferredTerminal = PreferredTerminal.ITerm,
            openInNewWindow = true,
            onError = { errors.add(it) },
        )

        assertEquals("applescript/terminal_new_window.applescript", runner.scriptInvocations.single().resourcePath)
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("iTerm", ignoreCase = true))
    }

    @Test
    fun `applescript failure is surfaced when surfaceAppleScriptErrors is enabled`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake(
            result = ScriptRunResult(exitCode = 1, stdout = "", stderr = "boom"),
        )
        val errors = mutableListOf<String>()
        service(runner = runner, surfaceAppleScriptErrors = true).openTerminal(
            workingDirectory = "/dir",
            command = null,
            preferredTerminal = PreferredTerminal.TerminalApp,
            openInNewWindow = true,
            onError = { errors.add(it) },
        )

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("boom"))
    }

    @Test
    fun `applescript failure is swallowed when surfaceAppleScriptErrors is disabled`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake(
            result = ScriptRunResult(exitCode = 1, stdout = "", stderr = "boom"),
        )
        val errors = mutableListOf<String>()
        service(runner = runner, surfaceAppleScriptErrors = false).openTerminal(
            workingDirectory = "/dir",
            command = null,
            preferredTerminal = PreferredTerminal.TerminalApp,
            openInNewWindow = true,
            onError = { errors.add(it) },
        )

        assertTrue(errors.isEmpty())
    }
}
