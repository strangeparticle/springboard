package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingItem
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto
import com.strangeparticle.springboard.app.unit.settings.persistence.SettingsPersistenceManagerInMemory
import kotlin.test.*

class SettingsManagerTest {

    private fun createManager(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        persistedDto: UserSettingsDto? = null,
        envVars: Map<String, String> = emptyMap(),
        cliArgs: List<String> = emptyList(),
    ): SettingsManager {
        val persistence = SettingsPersistenceManagerInMemory()
        if (persistedDto != null) {
            persistence.saveDto(persistedDto)
        }
        val manager = SettingsManager(target, persistence)
        manager.loadSettingsAtStartup(envVars, cliArgs)
        return manager
    }

    // -- Default Values --

    @Test
    fun `default boolean values`() {
        val manager = createManager()
        assertTrue(manager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE))
        assertTrue(manager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE))
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertTrue(manager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION))
        assertTrue(manager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION))
    }

    @Test
    fun `default file path is null`() {
        val manager = createManager()
        assertNull(manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun `default source is app default`() {
        val manager = createManager()
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- User Settings Precedence --

    @Test
    fun `user settings override defaults`() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = true)
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `user settings file path`() {
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
    fun `env var overrides user settings`() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = false),
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `env var overrides default`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- CLI Precedence --

    @Test
    fun `cli overrides env var`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "false"),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.COMMAND_LINE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `cli file path arg`() {
        val manager = createManager(
            cliArgs = listOf("--startup-springboard", "/cli/path.json"),
        )
        val filePath = manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNotNull(filePath)
        assertEquals("/cli/path.json", filePath.path)
        assertEquals(SettingsSource.COMMAND_LINE, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun `positional cli path does not set startup springboard`() {
        val manager = createManager(
            cliArgs = listOf("/cli/path.json"),
        )
        assertNull(manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun `cli boolean flag is present means true`() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- Override Detection --

    @Test
    fun `is overridden when env var set`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is overridden when cli set`() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is not overridden for default`() {
        val manager = createManager()
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is not overridden for user setting`() {
        val manager = createManager(
            persistedDto = UserSettingsDto(surfaceAppleScriptErrors = true),
        )
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- User Settings Mutation --

    @Test
    fun `set user setting`() {
        val manager = createManager()
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `set user setting persists`() {
        val persistence = SettingsPersistenceManagerInMemory()
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, persistence)
        manager.loadSettingsAtStartup()

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)

        val savedDto = persistence.currentDto()
        assertNotNull(savedDto)
        assertEquals(true, savedDto.surfaceAppleScriptErrors)
    }

    @Test
    fun `set user setting does not override higher precedence`() {
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
    fun `applicable settings for wasm excludes desktop settings`() {
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
    fun `applicable settings for desktop osx includes all settings`() {
        val manager = createManager(target = RuntimeEnvironment.DesktopOsx)
        val applicable = manager.applicableSettings()
        assertEquals(SettingsKey.entries.size, applicable.size)
    }

    // -- Type Safety --

    @Test
    fun `get boolean on file path throws`() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getBoolean(SettingsKey.STARTUP_SPRINGBOARD)
        }
    }

    @Test
    fun `get file path on boolean throws`() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getFilePath(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        }
    }

    // -- Coercion --

    @Test
    fun `coerce string to boolean`() {
        val entry = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertEquals(true, SettingsManager.coerceStringValue(entry, "true"))
        assertEquals(false, SettingsManager.coerceStringValue(entry, "false"))
    }

    @Test
    fun `coerce invalid string to boolean throws`() {
        val entry = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertFailsWith<IllegalArgumentException> {
            SettingsManager.coerceStringValue(entry, "maybe")
        }
    }

    @Test
    fun `coerce string to file path`() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_SPRINGBOARD)
        val result = SettingsManager.coerceStringValue(entry, "/some/path.json")
        assertEquals(FilePath("/some/path.json"), result)
    }

    @Test
    fun `coerce blank string to file path returns null`() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_SPRINGBOARD)
        val result = SettingsManager.coerceStringValue(entry, "  ")
        assertNull(result)
    }

    // -- Invalid External Values --

    @Test
    fun `invalid env var is ignored`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "not-a-boolean"),
        )
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }
}
