package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.platform.DesktopBrowser
import com.strangeparticle.springboard.app.platform.ScriptRunResult
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class ActivatorDesktopTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val focusRequester: FocusRequester,
    val activationService: PlatformActivationServiceDesktopTestFake,
)

@OptIn(ExperimentalTestApi::class)
object ActivatorDesktopTestScenarios {

    private fun createTestComponents(
        browser: DesktopBrowser = DesktopBrowser.Safari,
        scriptResult: ScriptRunResult = ScriptRunResult(exitCode = 0, stdout = "", stderr = ""),
        scriptException: Exception? = null,
        surfaceAppleScriptErrors: Boolean = false,
        executeCommandException: Exception? = null,
    ): ActivatorDesktopTestComponents {
        val browserDetection = PlatformBrowserDetectionServiceInMemoryFake(browser)
        val appleScriptRunner = PlatformAppleScriptRunnerServiceInMemoryFake(
            result = scriptResult,
            exception = scriptException,
        )
        val activationService = PlatformActivationServiceDesktopTestFake(
            browserDetectionService = browserDetection,
            appleScriptRunnerService = appleScriptRunner,
            surfaceAppleScriptErrors = surfaceAppleScriptErrors,
            executeCommandException = executeCommandException,
        )
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager, activationService)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        val focusRequester = FocusRequester()
        return ActivatorDesktopTestComponents(viewModel, settingsViewModel, focusRequester, activationService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: ActivatorDesktopTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
            )
        }
    }

    // --- Command activation ---

    fun commandActivatorRunsValidCommand() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/test/springboard.json")
        waitForIdle()

        // Activate the command activator cell
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertEquals(1, components.activationService.executedCommands.size)
        assertEquals("echo test", components.activationService.executedCommands.first())
    }

    fun commandActivatorShowsErrorForUnsuccessfulCommand() = runComposeUiTest {
        val components = createTestComponents(
            executeCommandException = RuntimeException("command not found"),
        )
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/test/springboard.json")
        waitForIdle()

        // Dismiss the security warning toast first
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(5000)
        waitForIdle()
        mainClock.autoAdvance = true
        waitForIdle()

        // Activate the command activator — executeCommand will throw
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
    }

    // --- Safari new window ---

    fun singleUrlActivationOpensANewSafariWindow() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Safari)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Single cell click activates one URL
        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertEquals(1, components.activationService.appleScriptRunnerService.scriptsRun.size)
        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.first().contains("safari"))
        assertEquals(1, components.activationService.openedUrls.size)
    }

    fun multiUrlActivationOpensOneNewSafariWindowWithAllUrls() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Safari)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        // Column activation — app1 in "all" env has res1 + res2
        components.viewModel.activateColumn("app1")
        waitForIdle()

        // One new window script call, two URLs opened
        assertEquals(1, components.activationService.appleScriptRunnerService.scriptsRun.size)
        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.first().contains("safari"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Chrome new window ---

    fun singleUrlActivationOpensANewChromeWindow() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Chrome)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        assertEquals(1, components.activationService.appleScriptRunnerService.scriptsRun.size)
        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.first().contains("chrome"))
        assertEquals(1, components.activationService.openedUrls.size)
    }

    fun multiUrlActivationOpensOneNewChromeWindowWithAllUrls() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Chrome)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.activateColumn("app1")
        waitForIdle()

        assertEquals(1, components.activationService.appleScriptRunnerService.scriptsRun.size)
        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.first().contains("chrome"))
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Unsupported browser fallback ---

    fun singleUrlFallsBackToNormalOpeningForUnsupportedBrowser() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Unsupported)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        // No AppleScript should run for unsupported browser
        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.isEmpty())
        // URL still opens via normal path
        assertEquals(1, components.activationService.openedUrls.size)
    }

    fun multiUrlFallsBackToNormalOpeningForUnsupportedBrowser() = runComposeUiTest {
        val components = createTestComponents(browser = DesktopBrowser.Unsupported)
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        components.viewModel.activateColumn("app1")
        waitForIdle()

        assertTrue(components.activationService.appleScriptRunnerService.scriptsRun.isEmpty())
        assertEquals(2, components.activationService.openedUrls.size)
    }

    // --- Broken browser integration fallback ---

    fun brokenBrowserIntegrationFallsBackToNormalOpeningWhenFallbackEnabled() = runComposeUiTest {
        val components = createTestComponents(
            browser = DesktopBrowser.Safari,
            scriptResult = ScriptRunResult(exitCode = 1, stdout = "", stderr = "script error"),
            surfaceAppleScriptErrors = false,
        )
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MULTI_ENV_WITH_ALL, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.gridCell("app1", "res1")).performClick()
        waitForIdle()

        // Script was attempted but failed
        assertEquals(1, components.activationService.appleScriptRunnerService.scriptsRun.size)
        // URL still opens (fallback — no error surfaced)
        assertEquals(1, components.activationService.openedUrls.size)
        // No error toast since surfaceAppleScriptErrors is false
        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertDoesNotExist()
    }
}
