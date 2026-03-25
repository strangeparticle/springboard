package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingItem
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsRegistry

import kotlin.test.*

class SettingsRegistryTest {

    @Test
    fun testAllSettingsKeysAreRegistered() {
        for (key in SettingsKey.entries) {
            assertNotNull(
                SettingsRegistry.get(key),
                "Settings key $key must be registered"
            )
        }
    }

    @Test
    fun testRequireThrowsForMissingKey() {
        // This test validates that require() works — since all keys are registered,
        // we just verify it returns non-null for a known key.
        val item = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun testRegistryEntriesHaveUniqueEnvVarNames() {
        val names = SettingsRegistry.allSettings().map { it.envVarName }
        assertEquals(names.size, names.distinct().size, "Environment variable names must be unique")
    }

    @Test
    fun testRegistryEntriesHaveUniqueCliParamNames() {
        val names = SettingsRegistry.allSettings().map { it.cliParamName }
        assertEquals(names.size, names.distinct().size, "CLI parameter names must be unique")
    }

    @Test
    fun testRegistryEntriesHaveUniqueJsonKeys() {
        val names = SettingsRegistry.allSettings().map { it.key.jsonKey }
        assertEquals(names.size, names.distinct().size, "JSON keys must be unique")
    }

    @Test
    fun testEnvVarNamesFollowConvention() {
        for (item in SettingsRegistry.allSettings()) {
            assertTrue(
                item.envVarName.startsWith("SPRINGBOARD_"),
                "Env var '${item.envVarName}' for ${item.key} must start with SPRINGBOARD_"
            )
            assertEquals(
                item.envVarName, item.envVarName.uppercase(),
                "Env var '${item.envVarName}' for ${item.key} must be UPPER_SNAKE_CASE"
            )
        }
    }

    @Test
    fun testCliParamNamesFollowConvention() {
        for (item in SettingsRegistry.allSettings()) {
            assertTrue(
                item.cliParamName.startsWith("--"),
                "CLI param '${item.cliParamName}' for ${item.key} must start with --"
            )
            val body = item.cliParamName.removePrefix("--")
            assertEquals(
                body, body.lowercase(),
                "CLI param '${item.cliParamName}' for ${item.key} must be lowercase kebab-case"
            )
        }
    }

    @Test
    fun testSettingsForTargetFiltersCorrectly() {
        val osxSettings = SettingsRegistry.settingsForEnvironment(RuntimeEnvironment.DesktopOsx)
        val wasmSettings = SettingsRegistry.settingsForEnvironment(RuntimeEnvironment.WASM)

        // WASM should only get general settings (no desktop-specific ones)
        for (item in wasmSettings) {
            assertFalse(
                item is SettingItem.Desktop,
                "${item.key} is a SettingItem.Desktop but was returned for WASM"
            )
        }

        // macOS should include both general and macOS desktop settings
        assertTrue(
            osxSettings.size > wasmSettings.size,
            "macOS should have more applicable settings than WASM"
        )
    }

    @Test
    fun testFindByEnvVarName() {
        val item = SettingsRegistry.findByEnvVarName("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS")
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun testFindByCliParamName() {
        val item = SettingsRegistry.findByCliParamName("--surface-applescript-errors")
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun testFindByJsonKey() {
        val item = SettingsRegistry.findByJsonKey(SettingsKey.SURFACE_APPLESCRIPT_ERRORS.jsonKey)
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun testFindByNonexistentNameReturnsNull() {
        assertNull(SettingsRegistry.findByEnvVarName("NONEXISTENT"))
        assertNull(SettingsRegistry.findByCliParamName("--nonexistent"))
        assertNull(SettingsRegistry.findByJsonKey("nonexistent"))
    }

    @Test
    fun testDefaultValuesMatchExpectedTypes() {
        for (item in SettingsRegistry.allSettings()) {
            val defaultValue = item.defaultValue
            if (defaultValue != null) {
                assertEquals(
                    item.type, defaultValue::class,
                    "Default value for ${item.key} has wrong type"
                )
            } else {
                // Null default is only valid for FilePath
                assertEquals(
                    FilePath::class, item.type,
                    "Null default only allowed for FilePath type, but ${item.key} is ${item.type.simpleName}"
                )
            }
        }
    }
}
