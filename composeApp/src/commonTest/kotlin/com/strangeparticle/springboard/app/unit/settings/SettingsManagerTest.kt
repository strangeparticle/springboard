package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingItem
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import kotlin.test.*

class SettingsManagerTest {

    private fun createManager(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        persistedDto: SettingsDto? = null,
        envVars: Map<String, String> = emptyMap(),
        cliArgs: List<String> = emptyList(),
    ): SettingsManager {
        val persistence = PersistenceServiceInMemoryFake()
        if (persistedDto != null) {
            persistence.persistSettings(persistedDto)
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

    // -- Precedence: USER > PARAMS > ENV > DEFAULT --

    @Test
    fun `user settings override defaults`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = true)
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `user settings file path`() {
        val manager = createManager(
            persistedDto = SettingsDto(startupSpringboard = "/path/to/file.json")
        )
        val filePath = manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNotNull(filePath)
        assertEquals("/path/to/file.json", filePath.path)
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun `user settings override env var`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = false),
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `user settings override params`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = false),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `env var overrides default`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `params override env var`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "false"),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.PARAMS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `params file path arg`() {
        val manager = createManager(
            cliArgs = listOf("--startup-springboard", "/cli/path.json"),
        )
        val filePath = manager.getFilePath(SettingsKey.STARTUP_SPRINGBOARD)
        assertNotNull(filePath)
        assertEquals("/cli/path.json", filePath.path)
        assertEquals(SettingsSource.PARAMS, manager.getSource(SettingsKey.STARTUP_SPRINGBOARD))
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

    // -- Override Detection (isOverridden = user has explicitly set value) --

    @Test
    fun `is not overridden when only env var set`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is not overridden when only params set`() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is not overridden for default`() {
        val manager = createManager()
        assertFalse(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `is overridden when user setting exists`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = true),
        )
        assertTrue(manager.isOverridden(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    // -- Source Tracking --

    @Test
    fun `getEffectiveSource returns highest priority source`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = true),
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "false"),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertEquals(SettingsSource.USER_SETTINGS, manager.getEffectiveSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
    }

    @Test
    fun `getValueFromSource returns value for specific source`() {
        val manager = createManager(
            persistedDto = SettingsDto(surfaceAppleScriptErrors = false),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertEquals(false, manager.getValueFromSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, SettingsSource.USER_SETTINGS))
        assertEquals(true, manager.getValueFromSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, SettingsSource.PARAMS))
    }

    @Test
    fun `getValueFromSource returns null when source has no value`() {
        val manager = createManager()
        assertNull(manager.getValueFromSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, SettingsSource.PARAMS))
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
        val persistence = PersistenceServiceInMemoryFake()
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, persistence)
        manager.loadSettingsAtStartup()

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)

        val savedDto = persistence.currentSettings()
        assertNotNull(savedDto)
        assertEquals(true, savedDto.surfaceAppleScriptErrors)
    }

    @Test
    fun `user setting overrides params`() {
        val manager = createManager(
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))

        manager.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, false)
        assertFalse(manager.getBoolean(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
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
