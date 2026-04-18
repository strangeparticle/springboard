package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
import kotlin.test.*

class SettingsKeyNamingTest {

    @Test
    fun `urlParamName produces lowercase kebab-case`() {
        assertEquals("startup-tabs", SettingsKeyNaming.urlParamName(SettingsKey.STARTUP_TABS))
        assertEquals("surface-applescript-errors", SettingsKeyNaming.urlParamName(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals("active-brand", SettingsKeyNaming.urlParamName(SettingsKey.ACTIVE_BRAND))
        assertEquals(
            "reset-key-nav-after-key-nav-activation",
            SettingsKeyNaming.urlParamName(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION),
        )
    }

    @Test
    fun `cliFlag prepends double dash to param name`() {
        assertEquals("--startup-tabs", SettingsKeyNaming.cliFlag(SettingsKey.STARTUP_TABS))
        assertEquals("--active-brand", SettingsKeyNaming.cliFlag(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `envVarName produces SPRINGBOARD prefixed UPPER_SNAKE_CASE`() {
        assertEquals("SPRINGBOARD_STARTUP_TABS", SettingsKeyNaming.envVarName(SettingsKey.STARTUP_TABS))
        assertEquals("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS", SettingsKeyNaming.envVarName(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals("SPRINGBOARD_ACTIVE_BRAND", SettingsKeyNaming.envVarName(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `jsonKey produces camelCase`() {
        assertEquals("startupTabs", SettingsKeyNaming.jsonKey(SettingsKey.STARTUP_TABS))
        assertEquals("surfaceApplescriptErrors", SettingsKeyNaming.jsonKey(SettingsKey.SURFACE_APPLESCRIPT_ERRORS))
        assertEquals("activeBrand", SettingsKeyNaming.jsonKey(SettingsKey.ACTIVE_BRAND))
        assertEquals(
            "resetKeyNavAfterKeyNavActivation",
            SettingsKeyNaming.jsonKey(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION),
        )
    }

    @Test
    fun `all keys produce unique url param names`() {
        val names = SettingsKey.entries.map { SettingsKeyNaming.urlParamName(it) }
        assertEquals(names.size, names.distinct().size, "URL param names must be unique")
    }

    @Test
    fun `all keys produce unique env var names`() {
        val names = SettingsKey.entries.map { SettingsKeyNaming.envVarName(it) }
        assertEquals(names.size, names.distinct().size, "Env var names must be unique")
    }

    @Test
    fun `all keys produce unique json keys`() {
        val names = SettingsKey.entries.map { SettingsKeyNaming.jsonKey(it) }
        assertEquals(names.size, names.distinct().size, "JSON keys must be unique")
    }
}
