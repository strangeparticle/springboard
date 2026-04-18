package com.strangeparticle.springboard.app.unit.settings

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
    fun `default startup tabs is empty`() {
        val manager = createManager()
        assertEquals(emptyList<String>(), manager.getStringList(SettingsKey.STARTUP_TABS))
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
    fun `user settings startup tabs`() {
        val manager = createManager(
            persistedDto = SettingsDto(startupTabs = listOf("/path/to/file.json"))
        )
        val tabs = manager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/path/to/file.json"), tabs)
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.STARTUP_TABS))
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
    fun `params startup tabs from cli`() {
        val manager = createManager(
            cliArgs = listOf("--startup-tabs", "/a.json,/b.json"),
        )
        val tabs = manager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/a.json", "/b.json"), tabs)
        assertEquals(SettingsSource.PARAMS, manager.getSource(SettingsKey.STARTUP_TABS))
    }

    @Test
    fun `positional cli path does not set startup tabs`() {
        val manager = createManager(
            cliArgs = listOf("/cli/path.json"),
        )
        assertEquals(emptyList<String>(), manager.getStringList(SettingsKey.STARTUP_TABS))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.STARTUP_TABS))
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
    fun `get boolean on list type throws`() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getBoolean(SettingsKey.STARTUP_TABS)
        }
    }

    @Test
    fun `get string list on boolean throws`() {
        val manager = createManager()
        assertFailsWith<IllegalArgumentException> {
            manager.getStringList(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
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
    fun `coerce comma-delimited string to list`() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_TABS)
        val result = SettingsManager.coerceStringValue(entry, "/a.json,/b.json")
        assertEquals(listOf("/a.json", "/b.json"), result)
    }

    @Test
    fun `coerce blank string to list returns empty`() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_TABS)
        val result = SettingsManager.coerceStringValue(entry, "  ")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `coerce single value string to list`() {
        val entry = SettingsRegistry.require(SettingsKey.STARTUP_TABS)
        val result = SettingsManager.coerceStringValue(entry, "/single.json")
        assertEquals(listOf("/single.json"), result)
    }

    // -- List Coercion from Env Var --

    @Test
    fun `env var sets startup tabs via comma-delimited string`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_STARTUP_TABS" to "/env/a.json,/env/b.json"),
        )
        val tabs = manager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/env/a.json", "/env/b.json"), tabs)
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SettingsKey.STARTUP_TABS))
    }

    @Test
    fun `cli startup tabs override env var startup tabs`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_STARTUP_TABS" to "/env/a.json"),
            cliArgs = listOf("--startup-tabs", "/cli/a.json,/cli/b.json"),
        )
        val tabs = manager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/cli/a.json", "/cli/b.json"), tabs)
        assertEquals(SettingsSource.PARAMS, manager.getSource(SettingsKey.STARTUP_TABS))
    }

    @Test
    fun `user startup tabs override cli startup tabs`() {
        val manager = createManager(
            persistedDto = SettingsDto(startupTabs = listOf("/user/a.json")),
            cliArgs = listOf("--startup-tabs", "/cli/a.json,/cli/b.json"),
        )
        val tabs = manager.getStringList(SettingsKey.STARTUP_TABS)
        assertEquals(listOf("/user/a.json"), tabs)
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.STARTUP_TABS))
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
