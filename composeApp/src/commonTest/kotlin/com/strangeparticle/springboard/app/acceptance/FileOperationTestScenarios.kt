package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

private data class FileOperationTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val fileContentService: PlatformFileContentService,
)

@OptIn(ExperimentalTestApi::class)
object FileOperationTestScenarios {

    private fun createTestComponents(
        fileContentService: PlatformFileContentService = PlatformFileContentServiceInMemoryFake(),
    ): FileOperationTestComponents {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SpringboardViewModel(settingsManager)
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        return FileOperationTestComponents(viewModel, settingsViewModel, fileContentService)
    }

    private fun ComposeUiTest.setSpringboardApp(components: FileOperationTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                fileContentService = components.fileContentService,
            )
        }
    }

    // --- Open valid springboard ---

    fun openingAValidSpringboardLoadsItSuccessfully() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE).assertExists().assertIsDisplayed()
    }

    // --- Open shows file path and time ---

    fun openingASpringboardShowsFilePathInStatusBar() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        val filePath = "/Users/test/my-springboard.json"
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, filePath)
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains(filePath, substring = true)
    }

    fun openingASpringboardShowsOpenTimeInStatusBar() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains("@", substring = true)
    }

    // --- Open error handling ---

    fun openingAnEmptyFileShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig("", "/test/empty.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
    }

    fun openingAFileWithMalformedJsonShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.MALFORMED_JSON, "/test/bad.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
    }

    fun openingAFileWithInvalidIdShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.INVALID_ACTIVATOR_REFERENCE, "/test/invalid.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
    }

    // --- Reload updates content ---

    fun reloadingReflectsNewFileContent() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/original.json")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertTextContains("/test/original.json", substring = true)

        // Simulate reload with different content from a different source
        components.viewModel.loadConfig(TestFixtureJson.ALTERNATIVE_URL_ONLY, "/test/updated.json")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertTextContains("/test/updated.json", substring = true)
    }

    fun reloadingUpdatesTheOpenTimeInStatusBar() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        // After reload, the status bar should still show the timestamp separator
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains("@", substring = true)
    }

    // --- Reload error handling ---

    fun reloadingAnEmptiedFileShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        // Simulate reload with empty content (file was emptied)
        components.viewModel.loadConfig("", "/test/springboard.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
        // Original springboard data should remain displayed
        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
    }

    fun reloadingAFileWithMalformedJsonShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.MALFORMED_JSON, "/test/springboard.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
        // Original springboard data should remain displayed
        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
    }

    fun reloadingAFileWithInvalidIdShowsError() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.INVALID_ACTIVATOR_REFERENCE, "/test/springboard.json")
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
        // Original springboard data should remain displayed
        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
    }

    fun reloadingADeletedFileShowsError() = runComposeUiTest {
        val fakeService = PlatformFileContentServiceInMemoryFake()
        val components = createTestComponents(fileContentService = fakeService)
        setSpringboardApp(components)
        waitForIdle()

        // Load a springboard via ViewModel so StatusBar renders
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        // Do not register any content for the path — simulates a deleted file
        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()
        waitForIdle()

        // Advance past the minimum spin duration so the reload coroutine completes
        mainClock.advanceTimeBy(CommonUiConstants.ReloadSpinMinMs + 100)
        waitForIdle()

        onNode(hasTestTag(TestTags.TOAST_SEVERITY_LABEL) and hasText("Error"))
            .assertExists()
    }

    // --- Load embedded ---

    fun loadEmbeddedLoadsTheBuiltInSpringboard() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "(embedded)")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE).assertExists().assertIsDisplayed()
    }

    fun loadEmbeddedUpdatesStatusLineToEmbeddedPath() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "(embedded)")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains("(embedded)", substring = true)
    }

    fun loadEmbeddedUpdatesTheOpenTimeInStatusBar() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "(embedded)")
        waitForIdle()

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE)
            .assertExists()
            .assertTextContains("@", substring = true)
    }

    // --- Reload button ---

    fun reloadButtonShowsCorrectIcon() = runComposeUiTest {
        val components = createTestComponents()
        setSpringboardApp(components)
        waitForIdle()
        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).assertExists().assertIsDisplayed()
    }

    fun reloadButtonSpinsDuringReload() = runComposeUiTest {
        val fakeService = PlatformFileContentServiceInMemoryFake()
        fakeService.fileContents["/test/springboard.json"] = TestFixtureJson.URL_ONLY
        val components = createTestComponents(fileContentService = fakeService)
        setSpringboardApp(components)
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        mainClock.autoAdvance = false
        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()
        // Advance enough for the coroutine to start but not complete the min spin delay
        mainClock.advanceTimeBy(50)

        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsNotEnabled()
    }

    fun reloadButtonSpinsForAtLeastTheMinimumDuration() = runComposeUiTest {
        val fakeService = PlatformFileContentServiceInMemoryFake()
        fakeService.fileContents["/test/springboard.json"] = TestFixtureJson.URL_ONLY
        val components = createTestComponents(fileContentService = fakeService)
        setSpringboardApp(components)
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        mainClock.autoAdvance = false
        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()

        // Advance to just before the minimum spin duration — button should still be disabled
        mainClock.advanceTimeBy(CommonUiConstants.ReloadSpinMinMs - 100)
        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsNotEnabled()

        // Advance past the minimum — button should re-enable
        mainClock.advanceTimeBy(200)
        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsEnabled()
    }

    fun reloadButtonStopsSpinningAfterSuccessfulReload() = runComposeUiTest {
        val fakeService = PlatformFileContentServiceInMemoryFake()
        fakeService.fileContents["/test/springboard.json"] = TestFixtureJson.URL_ONLY
        val components = createTestComponents(fileContentService = fakeService)
        setSpringboardApp(components)
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()

        // Advance past the minimum spin duration
        mainClock.advanceTimeBy(CommonUiConstants.ReloadSpinMinMs + 100)
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsEnabled()
    }

    fun reloadButtonStopsSpinningAfterFailedReload() = runComposeUiTest {
        val fakeService = PlatformFileContentServiceInMemoryFake()
        val components = createTestComponents(fileContentService = fakeService)
        setSpringboardApp(components)
        waitForIdle()

        components.viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        waitForIdle()

        // Don't register content — reload will fail (file not found)
        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()

        // Advance past the minimum spin duration
        mainClock.advanceTimeBy(CommonUiConstants.ReloadSpinMinMs + 100)
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsEnabled()
    }

}
