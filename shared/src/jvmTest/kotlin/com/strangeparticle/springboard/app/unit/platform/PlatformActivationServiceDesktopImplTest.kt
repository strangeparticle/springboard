package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.acceptance.PlatformAppleScriptRunnerServiceInMemoryFake
import com.strangeparticle.springboard.app.acceptance.PlatformBrowserDetectionServiceInMemoryFake
import com.strangeparticle.springboard.app.platform.PlatformActivationServiceDesktopImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformActivationServiceDesktopImplTest {

    @Test
    fun `hideApplicationViaPid runs hide_application_via_pid applescript with current jvm pid`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake()
        val service = PlatformActivationServiceDesktopImpl(
            browserDetectionService = PlatformBrowserDetectionServiceInMemoryFake(),
            appleScriptRunnerService = runner,
        )

        service.hideApplicationViaPid()

        assertEquals(1, runner.scriptInvocations.size)
        val invocation = runner.scriptInvocations.first()
        assertEquals("applescript/hide_application_via_pid.applescript", invocation.resourcePath)
        assertEquals(listOf(ProcessHandle.current().pid().toString()), invocation.args)
    }

    @Test
    fun `hideApplicationViaPid swallows runner errors`() {
        val runner = PlatformAppleScriptRunnerServiceInMemoryFake(
            exception = RuntimeException("osascript blew up"),
        )
        val service = PlatformActivationServiceDesktopImpl(
            browserDetectionService = PlatformBrowserDetectionServiceInMemoryFake(),
            appleScriptRunnerService = runner,
        )

        service.hideApplicationViaPid()

        assertEquals(1, runner.scriptInvocations.size)
    }
}
