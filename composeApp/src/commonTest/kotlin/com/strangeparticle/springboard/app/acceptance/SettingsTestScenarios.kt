package com.strangeparticle.springboard.app.acceptance

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.ui.SpringboardApp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class SettingsTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val settingsManager: SettingsManager,
    val persistenceService: PersistenceServiceInMemoryFake,
    val focusRequester: FocusRequester,
    val showSettings: MutableState<Boolean>,
    val showActiveSettings: MutableState<Boolean>,
)

@OptIn(ExperimentalTestApi::class)
object SettingsTestScenarios {

    private fun createTestComponents(
        runtimeEnvironment: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        environmentVariables: Map<String, String> = emptyMap(),
        commandLineArgs: List<String> = emptyList(),
        persistenceService: PersistenceServiceInMemoryFake = PersistenceServiceInMemoryFake(),
        currentFilePath: String? = null,
        showSettings: MutableState<Boolean> = mutableStateOf(false),
        showActiveSettings: MutableState<Boolean> = mutableStateOf(false),
    ): SettingsTestComponents {
        val settingsManager = SettingsManager(runtimeEnvironment, persistenceService)
        settingsManager.loadSettingsAtStartup(environmentVariables, commandLineArgs)
        val activationService = PlatformActivationServiceInMemoryFake()
        val viewModel = SpringboardViewModel(settingsManager, PersistenceServiceInMemoryFake(), activationService)
        if (currentFilePath != null) {
            viewModel.loadConfig(TestFixtureJson.URL_ONLY, currentFilePath)
        }
        val settingsViewModel = SettingsViewModel(settingsManager) { viewModel.springboard?.source }
        val focusRequester = FocusRequester()
        return SettingsTestComponents(
            viewModel, settingsViewModel, settingsManager, persistenceService,
            focusRequester, showSettings, showActiveSettings,
        )
    }

    private fun ComposeUiTest.setSpringboardApp(components: SettingsTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
                showSettings = components.showSettings,
                showActiveSettings = components.showActiveSettings,
            )
        }
    }

    // --- Navigation ---

    fun statusBarGearIconOpensSettingsScreen() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
        )
        setSpringboardApp(components)
        waitForIdle()

        // Settings screen should not be visible initially
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertDoesNotExist()

        // Click gear icon
        onNodeWithTag(TestTags.SETTINGS_GEAR_ICON).performClick()
        waitForIdle()

        // Settings screen should now be visible
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertExists()
    }

    fun settingsBackButtonReturnsToMainScreen() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Should be on settings screen
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertExists()

        // Click back
        onNodeWithTag(TestTags.SETTINGS_BACK_BUTTON).performClick()
        waitForIdle()

        // Should be back on main screen
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertDoesNotExist()
        onNodeWithTag(TestTags.STATUS_BAR_SOURCE).assertExists()
    }

    fun activeSettingsBackButtonReturnsToSettingsWhenOpenedFromSettingsLink() = runComposeUiTest {
        val activeSettingsOpenedFromSettings = mutableStateOf(false)
        val showActiveSettings = mutableStateOf(false)
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
            showActiveSettings = showActiveSettings,
        )
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                firstDropdownFocusRequester = components.focusRequester,
                showSettings = components.showSettings,
                showActiveSettings = components.showActiveSettings,
                activeSettingsOpenedFromSettings = activeSettingsOpenedFromSettings,
            )
        }
        waitForIdle()

        // On settings screen — override warning should exist
        onNodeWithTag(TestTags.SETTINGS_OVERRIDE_WARNING).assertExists()

        // Navigate to active settings from settings (simulates the link click)
        activeSettingsOpenedFromSettings.value = true
        showActiveSettings.value = true
        waitForIdle()

        onNodeWithTag(TestTags.ACTIVE_SETTINGS_SCREEN).assertExists()

        // Click active settings back button
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_BACK_BUTTON).performClick()
        waitForIdle()

        // Should return to settings screen (not main)
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertExists()
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_SCREEN).assertDoesNotExist()
    }

    fun activeSettingsBackButtonReturnsToPreviousScreenWhenOpenedDirectly() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Should be on active settings screen (opened directly, not from settings link)
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_SCREEN).assertExists()

        // Click back
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_BACK_BUTTON).performClick()
        waitForIdle()

        // Should return to main screen (not settings), since active settings was opened directly
        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertDoesNotExist()
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_SCREEN).assertDoesNotExist()
        onNodeWithTag(TestTags.STATUS_BAR_SOURCE).assertExists()
    }

    // --- Override behavior ---

    fun overriddenSettingsAppearVisuallyDisabled() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // The overridden setting should exist and show the override warning
        onNodeWithTag(TestTags.SETTINGS_OVERRIDE_WARNING).assertExists()

        // Verify the setting is overridden at the ViewModel level
        assertTrue(components.settingsViewModel.isOverridden(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
    }

    fun overriddenSettingsShowOverrideWarning() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Override warning should contain the source name
        onNode(hasTestTag(TestTags.SETTINGS_OVERRIDE_WARNING) and hasText("environment variable", substring = true))
            .assertExists()
    }

    fun overrideWarningLinkNavigatesToActiveSettings() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEYNAV_AFTER_KEYNAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Override warning exists and mentions Active Settings
        onNode(hasTestTag(TestTags.SETTINGS_OVERRIDE_WARNING) and hasText("Active Settings", substring = true))
            .assertExists()

        // Click the override warning text — the annotated string link should navigate
        onNodeWithTag(TestTags.SETTINGS_OVERRIDE_WARNING).performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        waitForIdle()

        // Should now be on Active Settings screen
        onNodeWithTag(TestTags.ACTIVE_SETTINGS_SCREEN).assertExists()
    }

    // --- Active settings source labels ---

    fun activeSettingsShowsDefaultSourceLabel() = runComposeUiTest {
        val components = createTestComponents(
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Settings with no user/env/CLI override show "Default" source
        onNode(hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("Default"))
            .assertExists()
    }

    fun activeSettingsShowsUserSourceLabel() = runComposeUiTest {
        val components = createTestComponents(
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        // Set a user setting before rendering
        components.settingsManager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        setSpringboardApp(components)
        waitForIdle()

        onNode(hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("User"))
            .assertExists()
    }

    fun activeSettingsShowsEnvVarSourceLabel() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true",
            ),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("Env var"))
            .assertExists()
    }

    fun activeSettingsShowsCommandLineSourceLabel() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--surface-applescript-errors"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("CLI"))
            .assertExists()
    }

    // --- Startup springboard path display ---

    fun activeSettingsShowsPathForStartupSpringboard() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-springboard", "/home/user/my-springboard.json"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // The value should display "path" (the formatted display text for FilePath)
        // TooltipBox wraps the text node, so we need useUnmergedTree
        onNode(
            hasTestTag(TestTags.activeSettingsValue("Startup Springboard")) and hasText("path"),
            useUnmergedTree = true,
        ).assertExists()
    }

    fun hoverTooltipRevealsFullStartupSpringboardPath() = runComposeUiTest {
        val testPath = "/home/user/my-springboard.json"
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-springboard", testPath),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // The tooltip text contains the full path — verify via the ViewModel
        val entry = components.settingsViewModel.activeSettingsEntries
            .first { it.displayName == "Startup Springboard" }
        assertEquals(testPath, entry.tooltipText)
    }

    // --- Startup springboard from command line ---

    fun startupSpringboardFlagSetsStartupSpringboard() = runComposeUiTest {
        val testPath = "/cli/startup.json"
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-springboard", testPath),
        )
        setSpringboardApp(components)
        waitForIdle()

        val resolved = components.settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertEquals(testPath, resolved?.path)
    }

    fun activeSettingsShowsCliAsSourceForStartupSpringboard() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-springboard", "/cli/startup.json"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(hasTestTag(TestTags.activeSettingsSourceLabel("Startup Springboard")) and hasText("CLI"))
            .assertExists()
    }

    // --- Positional path ---

    fun positionalCliPathDoesNotCountAsStartupSpringboardOverride() = runComposeUiTest {
        // A positional path (no --startup-springboard flag) should NOT set startup springboard
        val components = createTestComponents(
            commandLineArgs = listOf("/some/positional/path.json"),
        )
        setSpringboardApp(components)
        waitForIdle()

        val resolved = components.settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNull(resolved)
        assertEquals(SettingsSource.APP_DEFAULT, components.settingsManager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    // --- Use current file and clear ---

    fun useCurrentFileStoresCurrentFileAsStartupSpringboard() = runComposeUiTest {
        val testFilePath = "/test/my-springboard.json"
        val components = createTestComponents(
            currentFilePath = testFilePath,
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Click "Use Current File" button
        onNodeWithTag(TestTags.SETTINGS_USE_CURRENT_FILE_BUTTON).performClick()
        waitForIdle()

        // The startup springboard should now be set to the current file path
        val resolved = components.settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertEquals(testFilePath, resolved?.path)
        assertEquals(SettingsSource.USER_SETTINGS, components.settingsManager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    fun clearRemovesUserConfiguredStartupSpringboard() = runComposeUiTest {
        val testFilePath = "/test/my-springboard.json"
        val components = createTestComponents(
            currentFilePath = testFilePath,
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // First set a startup springboard
        onNodeWithTag(TestTags.SETTINGS_USE_CURRENT_FILE_BUTTON).performClick()
        waitForIdle()

        // Verify it was set
        assertEquals(testFilePath, components.settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)?.path)

        // Now click "Clear"
        onNodeWithTag(TestTags.SETTINGS_CLEAR_BUTTON).performClick()
        waitForIdle()

        // Startup springboard should be cleared
        assertNull(components.settingsManager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD))
    }

    // --- Settings persistence ---

    fun startupSpringboardSettingPersistsAcrossRelaunch() = runComposeUiTest {
        val persistenceService = PersistenceServiceInMemoryFake()
        val testFilePath = "/test/my-springboard.json"

        // First "session" — set startup springboard
        val components = createTestComponents(
            persistenceService = persistenceService,
            currentFilePath = testFilePath,
        )
        components.settingsViewModel.designateCurrentFileAsStartup()

        // Verify persisted
        val dto = persistenceService.currentSettings()
        assertEquals(testFilePath, dto?.startupSpringboard)

        // Second "session" — create new managers with same persistence
        val settingsManager2 = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager2.loadSettingsAtStartup()

        val resolved = settingsManager2.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertEquals(testFilePath, resolved?.path)
        assertEquals(SettingsSource.USER_SETTINGS, settingsManager2.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    fun surfaceAppleScriptErrorsSettingPersistsAcrossRelaunch() = runComposeUiTest {
        val persistenceService = PersistenceServiceInMemoryFake()

        // First "session" — toggle setting
        val components = createTestComponents(persistenceService = persistenceService)
        components.settingsViewModel.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)

        // Verify persisted
        assertEquals(true, persistenceService.currentSettings()?.surfaceAppleScriptErrors)

        // Second "session"
        val settingsManager2 = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager2.loadSettingsAtStartup()

        assertEquals(true, settingsManager2.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, settingsManager2.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    fun resetKeyNavAfterKeyNavActivationSettingPersistsAcrossRelaunch() = runComposeUiTest {
        val persistenceService = PersistenceServiceInMemoryFake()

        // First "session" — toggle setting off (default is true)
        val components = createTestComponents(persistenceService = persistenceService)
        components.settingsViewModel.setUserSetting(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION, false)

        assertEquals(false, persistenceService.currentSettings()?.resetKeyNavAfterKeyNavActivation)

        // Second "session"
        val settingsManager2 = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager2.loadSettingsAtStartup()

        assertEquals(false, settingsManager2.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
        assertEquals(SettingsSource.USER_SETTINGS, settingsManager2.getSource(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
    }

    // --- Active brand dropdown ---

    fun activeBrandDropdownIsPresent() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNodeWithTag(TestTags.settingsDropdown(SettingsKey.ACTIVE_BRAND.jsonKey)).assertExists()
    }

    fun selectingDarkBrandFromDropdownUpdatesActiveBrand() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        assertEquals("strangeparticle-light", components.settingsViewModel.activeBrandId)

        onNodeWithTag(TestTags.settingsDropdown(SettingsKey.ACTIVE_BRAND.jsonKey)).performClick()
        waitForIdle()

        onNodeWithTag(
            TestTags.settingsDropdownOption(SettingsKey.ACTIVE_BRAND.jsonKey, "strangeparticle-dark"),
        ).performClick()
        waitForIdle()

        assertEquals("strangeparticle-dark", components.settingsViewModel.activeBrandId)
        assertEquals(SettingsSource.USER_SETTINGS, components.settingsManager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    fun activeBrandDropdownShowsOverrideWarningWhenSetByCli() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--active-brand", "strangeparticle-dark"),
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        assertEquals("strangeparticle-dark", components.settingsViewModel.activeBrandId)
        assertTrue(components.settingsViewModel.isOverridden(SettingsKey.ACTIVE_BRAND))

        // The dropdown is still rendered — it's just disabled — but the row's override
        // warning should be present (there may be multiple override warnings on the
        // screen if other settings are overridden as well, so use assertAny).
        onAllNodesWithTag(TestTags.SETTINGS_OVERRIDE_WARNING).assertAny(
            hasText("command-line", substring = true),
        )
    }

    fun resetKeyNavAfterGridNavActivationSettingPersistsAcrossRelaunch() = runComposeUiTest {
        val persistenceService = PersistenceServiceInMemoryFake()

        // First "session" — toggle setting off (default is true)
        val components = createTestComponents(persistenceService = persistenceService)
        components.settingsViewModel.setUserSetting(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION, false)

        assertEquals(false, persistenceService.currentSettings()?.resetKeyNavAfterGridNavActivation)

        // Second "session"
        val settingsManager2 = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager2.loadSettingsAtStartup()

        assertEquals(false, settingsManager2.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION))
        assertEquals(SettingsSource.USER_SETTINGS, settingsManager2.getSource(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION))
    }
}
