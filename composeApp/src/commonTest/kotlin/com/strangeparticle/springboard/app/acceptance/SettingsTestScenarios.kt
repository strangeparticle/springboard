package com.strangeparticle.springboard.app.acceptance

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class SettingsTestComponents(
    val viewModel: SpringboardViewModel,
    val settingsViewModel: SettingsViewModel,
    val settingsManager: SettingsManager,
    val persistenceService: PersistenceServiceInMemoryFake,
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
        val settingsViewModel = SettingsViewModel(settingsManager)
        return SettingsTestComponents(
            viewModel, settingsViewModel, settingsManager, persistenceService,
            showSettings, showActiveSettings,
        )
    }

    private fun ComposeUiTest.setSpringboardApp(components: SettingsTestComponents) {
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
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
                "SPRINGBOARD_RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
            showActiveSettings = showActiveSettings,
        )
        setContent {
            SpringboardApp(
                viewModel = components.viewModel,
                settingsViewModel = components.settingsViewModel,
                showSettings = components.showSettings,
                showActiveSettings = components.showActiveSettings,
                activeSettingsOpenedFromSettings = activeSettingsOpenedFromSettings,
            )
        }
        waitForIdle()

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

    fun envVarSettingsAreNotMarkedAsOverridden() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // With inverted precedence, env var does NOT mean "overridden" — only user-set values are overridden
        assertFalse(components.settingsViewModel.isOverridden(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
    }

    fun envVarSettingsShowProvenanceIndicator() = runComposeUiTest {
        val components = createTestComponents(
            environmentVariables = mapOf(
                "SPRINGBOARD_RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION" to "false",
            ),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Provenance indicator should show "env var" as source
        onAllNodes(hasText("Source: env var", substring = true))
            .fetchSemanticsNodes().also { assertTrue(it.isNotEmpty()) }
    }

    fun provenanceLinkNavigatesToActiveSettings() = runComposeUiTest {
        val components = createTestComponents(
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Click the "Active Settings" header button to navigate
        onNode(hasText("Active Settings")).performClick()
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
        onNode(
            hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("Default"),
            useUnmergedTree = true,
        ).assertExists()
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

        onNode(
            hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("User (session)"),
            useUnmergedTree = true,
        ).assertExists()
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

        onNode(
            hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("Env var"),
            useUnmergedTree = true,
        ).assertExists()
    }

    fun activeSettingsShowsParamsSourceLabel() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--surface-applescript-errors"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(
            hasTestTag(TestTags.activeSettingsSourceLabel("Surface AppleScript errors")) and hasText("CLI flag"),
            useUnmergedTree = true,
        ).assertExists()
    }

    // --- Startup tabs display ---

    fun activeSettingsShowsStartupTabs() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-tabs", "/a.json,/b.json"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(
            hasTestTag(TestTags.activeSettingsValue("Startup Tabs")) and hasText("/a.json", substring = true),
            useUnmergedTree = true,
        ).assertExists()
    }

    fun activeSettingsShowsStartupTabsCommaSeparated() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-tabs", "/a.json,/b.json"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        val entry = components.settingsViewModel.activeSettingsEntries
            .first { it.displayName == "Startup Tabs" }
        assertTrue(entry.resolvedValue.contains("/a.json"))
        assertTrue(entry.resolvedValue.contains("/b.json"))
    }

    // --- Startup tabs from command line ---

    fun startupTabsFlagSetsStartupTabs() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-tabs", "/a.json,/b.json"),
        )
        setSpringboardApp(components)
        waitForIdle()

        val resolved = components.settingsManager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/a.json", "/b.json"), resolved)
    }

    fun activeSettingsShowsParamsAsSourceForStartupTabs() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--startup-tabs", "/cli/startup.json"),
            showSettings = mutableStateOf(true),
            showActiveSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNode(
            hasTestTag(TestTags.activeSettingsSourceLabel("Startup Tabs")) and hasText("CLI flag"),
            useUnmergedTree = true,
        ).assertExists()
    }

    // --- Positional path ---

    fun positionalCliPathDoesNotCountAsStartupTabsOverride() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("/some/positional/path.json"),
        )
        setSpringboardApp(components)
        waitForIdle()

        val resolved = components.settingsManager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(emptyList<String>(), resolved)
        assertEquals(SettingsSource.APP_DEFAULT, components.settingsManager.getSource(SettingsKey.STARTUP_TABS))
    }

    // --- Settings persistence ---

    fun startupTabsSettingPersistsAcrossRelaunch() = runComposeUiTest {
        val persistenceService = PersistenceServiceInMemoryFake()
        val testTabs = listOf("/test/a.json", "/test/b.json")

        // First "session" — set startup tabs
        val components = createTestComponents(
            persistenceService = persistenceService,
        )
        components.settingsManager.setUserSetting(SettingsKey.STARTUP_TABS, testTabs)

        // Verify persisted
        val dto = persistenceService.currentSettings()
        assertEquals(testTabs, dto?.startupTabs)

        // Second "session" — create new managers with same persistence
        val settingsManager2 = SettingsManager(RuntimeEnvironment.DesktopOsx, persistenceService)
        settingsManager2.loadSettingsAtStartup()

        val resolved = settingsManager2.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(testTabs, resolved)
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, settingsManager2.getSource(SettingsKey.STARTUP_TABS))
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
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, settingsManager2.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
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
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, settingsManager2.getSource(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
    }

    // --- Active brand dropdown ---

    fun activeBrandDropdownIsPresent() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        onNodeWithTag(TestTags.settingsDropdown(SettingsKeyNaming.jsonKey(SettingsKey.ACTIVE_BRAND))).assertExists()
    }

    fun selectingDarkBrandFromDropdownUpdatesActiveBrand() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        assertEquals("strangeparticle-light", components.settingsViewModel.activeBrandId)

        onNodeWithTag(TestTags.settingsDropdown(SettingsKeyNaming.jsonKey(SettingsKey.ACTIVE_BRAND))).performClick()
        waitForIdle()

        onNodeWithTag(
            TestTags.settingsDropdownOption(SettingsKeyNaming.jsonKey(SettingsKey.ACTIVE_BRAND), "strangeparticle-dark"),
        ).performClick()
        waitForIdle()

        assertEquals("strangeparticle-dark", components.settingsViewModel.activeBrandId)
        assertEquals(SettingsSource.USER_SETTINGS_FROM_SESSION, components.settingsManager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    fun activeBrandDropdownShowsProvenanceWhenSetByParams() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--active-brand", "strangeparticle-dark"),
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        assertEquals("strangeparticle-dark", components.settingsViewModel.activeBrandId)
        assertFalse(components.settingsViewModel.isOverridden(SettingsKey.ACTIVE_BRAND))

        // Provenance indicator shows "cli flag" as the source
        onAllNodes(hasText("Source: cli flag", substring = true))
            .fetchSemanticsNodes().also { assertTrue(it.isNotEmpty()) }
    }

    // --- Restore Defaults ---

    fun restoreDefaultsClearsAllUserSettings() = runComposeUiTest {
        val components = createTestComponents(
            commandLineArgs = listOf("--surface-applescript-errors"),
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        // Set a user setting
        components.settingsViewModel.setUserSetting(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION, false)
        assertTrue(components.settingsViewModel.isOverridden(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))

        // Click Restore Defaults
        onNodeWithTag(TestTags.SETTINGS_RESTORE_DEFAULTS_BUTTON).performClick()
        waitForIdle()

        // User setting should be cleared, falling through to default
        assertFalse(components.settingsViewModel.isOverridden(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
        assertTrue(components.settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))

        // Params-sourced value should still be active (not cleared by restore)
        assertTrue(components.settingsManager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.CLI_FLAG, components.settingsManager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    fun useCurrentTabsButtonSavesTabSources() = runComposeUiTest {
        val components = createTestComponents(
            currentFilePath = "/test/springboard.json",
            showSettings = mutableStateOf(true),
        )
        setSpringboardApp(components)
        waitForIdle()

        val tabSources = components.viewModel.currentTabSources
        assertTrue(tabSources.isNotEmpty())

        onNodeWithTag(TestTags.SETTINGS_USE_CURRENT_TABS_BUTTON).performClick()
        waitForIdle()

        val savedTabs = components.settingsManager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(tabSources, savedTabs)
        assertEquals(SettingsSource.USER_SETTINGS_FROM_SESSION, components.settingsManager.getSource(SettingsKey.STARTUP_TABS))
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
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, settingsManager2.getSource(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION))
    }
}
