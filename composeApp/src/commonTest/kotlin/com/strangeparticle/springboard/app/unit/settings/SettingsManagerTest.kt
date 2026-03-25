package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingItem
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto
import com.strangeparticle.springboard.app.unit.InMemorySettingsPersistenceManager
import kotlin.test.*

class SettingsManagerTest {

    private fun createManager(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        persistedDto: UserSettingsDto? = null,
        envVars: Map<String, String> = emptyMap(),
        cliArgs: List<String> = emptyList(),
    ): SettingsManager {
        val persistence = InMemorySettingsPersistenceManager()
        if (persistedDto != null) {
            persistence.saveDto(persistedDto)
        }
        val manager = SettingsManager(target, persistence)
        manager.loadSettingsAtStartup(envVars, cliArgs)
        return manager
    }

    // -- Default Values --

    @Test
    fun testDefaultBooleanValues() {
        val manager = createManager()
        assertTrue(manager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE))
        assertTrue(manager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE))
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertTrue(manager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
        assertTrue(manager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION))
    }

    @Test
    fun testDefaultFilePathIsNull() {
        val manager = createManager()
        assertNull(manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun testDefaultSourceIsAppDefault() {
        val manager = createManager()
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- User Settings Precedence --

    @Test
    fun testUserSettingsOverrideDefaults() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = true)
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testUserSettingsFilePath() {
        val manager = createManager(
            persistedDto = UserSettingsDto(startupSpringboard = "/path/to/file.json")
        )
        val filePath = manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNotNull(filePath)
        assertEquals("/path/to/file.json", filePath.path)
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    // -- Environment Variable Precedence --

    @Test
    fun testEnvVarOverridesUserSettings() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = false),
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testEnvVarOverridesDefault() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- CLI Precedence --

    @Test
    fun testCliOverridesEnvVar() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "false"),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.COMMAND_LINE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testCliFilePathArg() {
        val manager = createManager(
            cliArgs = listOf("--startup-springboard", "/cli/path.json"),
        )
        val filePath = manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNotNull(filePath)
        assertEquals("/cli/path.json", filePath.path)
        assertEquals(SettingsSource.COMMAND_LINE, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun testPositionalCliPathDoesNotSetStartupSpringboard() {
        val manager = createManager(
            cliArgs = listOf("/cli/path.json"),
        )
        assertNull(manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun testCliBooleanFlagIsPresentMeansTrue() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- Override Detection --

    @Test
    fun testIsOverriddenWhenEnvVarSet() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testIsOverriddenWhenCliSet() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testIsNotOverriddenForDefault() {
        val manager = createManager()
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testIsNotOverriddenForUserSetting() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = true),
        )
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- User Settings Mutation --

    @Test
    fun testSetUserSetting() {
        val manager = createManager()
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun testSetUserSettingPersists() {
        val persistence = InMemorySettingsPersistenceManager()
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, persistence)
        manager.loadSettingsAtStartup()

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)

        val savedDto = persistence.currentDto()
        assertNotNull(savedDto)
        assertEquals(true, savedDto.surfaceAppleScriptErrors)
    }

    @Test
    fun testSetUserSettingDoesNotOverrideHigherPrecedence() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, false)
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.COMMAND_LINE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- Target Filtering --

    @Test
    fun testApplicableSettingsForWasm() {
        val manager = createManager(target = RuntimeEnvironment.WASM)
        val applicable = manager.applicableSettings()
        for (item in applicable) {
            assertFalse(
                item is SettingItem.Desktop,
                "${item.key} should not be applicable on WASM"
            )
        }
    }

    @Test
    fun testApplicableSettingsForDesktopOsx() {
        val manager = createManager(target = RuntimeEnvironment.DesktopOsx)
        val applicable = manager.applicableSettings()
        assertEquals(SettingsKey.entries.size, applicable.size)
    }

    // -- Type Safety --

    @Test
    fun testGetBooleanOnFilePathThrows() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getBoolean(SettingsKey.STARTUP_SPRINGBOARD)
        }
    }

    @Test
    fun testGetFilePathOnBooleanThrows() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getFilePath(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        }
    }

    // -- Coercion --

    @Test
    fun testCoerceStringToBoolean() {
        val entry = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertEquals(true, SettingsManager.coerceStringValue(entry, "true"))
        assertEquals(false, SettingsManager.coerceStringValue(entry, "false"))
    }

    @Test
    fun testCoerceInvalidStringToBooleanThrows() {
        val entry = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertFailsWith<IllegalArgumentException> {
            SettingsManager.coerceStringValue(entry, "maybe")
        }
    }

    @Test
    fun testCoerceStringToFilePath() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_SPRINGBOARD)
        val result = SettingsManager.coerceStringValue(entry, "/some/path.json")
        assertEquals(FilePath("/some/path.json"), result)
    }

    @Test
    fun testCoerceBlankStringToFilePathReturnsNull() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_SPRINGBOARD)
        val result = SettingsManager.coerceStringValue(entry, "  ")
        assertNull(result)
    }

    // -- Invalid External Values --

    @Test
    fun testInvalidEnvVarIsIgnored() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "not-a-boolean"),
        )
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }
}
