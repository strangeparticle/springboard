package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingItem
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
import com.strangeparticle.springboard.app.settings.SettingsRegistry

import kotlin.test.*

class SettingsRegistryTest {

    @Test
    fun `all settings keys are registered`() {
        for (key in SettingsKey.entries) {
            assertNotNull(
                SettingsRegistry.get(key),
                "Settings key $key must be registered"
            )
        }
    }

    @Test
    fun `require throws for missing key`() {
        val item = SettingsRegistry.require(SettingsKey.SURFACE_APPLESCRIPT_ERRORS)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun `registry entries have unique json keys`() {
        val names = SettingsRegistry.allSettings().map { SettingsKeyNaming.jsonKey(it.key) }
        assertEquals(names.size, names.distinct().size, "JSON keys must be unique")
    }

    @Test
    fun `settings for target filters correctly`() {
        val osxSettings = SettingsRegistry.settingsForEnvironment(RuntimeEnvironment.DesktopOsx)
        val wasmSettings = SettingsRegistry.settingsForEnvironment(RuntimeEnvironment.WASM)

        for (item in wasmSettings) {
            assertFalse(
                item is SettingItem.Desktop,
                "${item.key} is a SettingItem.Desktop but was returned for WASM"
            )
        }

        assertTrue(
            osxSettings.size > wasmSettings.size,
            "macOS should have more applicable settings than WASM"
        )
    }

    @Test
    fun `find by env var name`() {
        val item = SettingsRegistry.findByEnvVarName("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS")
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun `find by url param name`() {
        val item = SettingsRegistry.findByUrlParamName("surface-applescript-errors")
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun `find by json key`() {
        val item = SettingsRegistry.findByJsonKey(SettingsKeyNaming.jsonKey(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertNotNull(item)
        assertEquals(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, item.key)
    }

    @Test
    fun `find by nonexistent name returns null`() {
        assertNull(SettingsRegistry.findByEnvVarName("NONEXISTENT"))
        assertNull(SettingsRegistry.findByUrlParamName("nonexistent"))
        assertNull(SettingsRegistry.findByJsonKey("nonexistent"))
    }

    @Test
    fun `default values match expected types`() {
        for (item in SettingsRegistry.allSettings()) {
            val defaultValue = item.defaultValue
            if (defaultValue != null) {
                if (item.type == List::class) {
                    assertTrue(
                        defaultValue is List<*>,
                        "Default value for ${item.key} should be a List but was ${defaultValue::class.simpleName}"
                    )
                } else {
                    assertEquals(
                        item.type, defaultValue::class,
                        "Default value for ${item.key} has wrong type"
                    )
                }
            } else {
                fail("Null default is not allowed — ${item.key} (${item.type.simpleName}) has a null default value")
            }
        }
    }
}
