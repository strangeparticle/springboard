package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.StringFromDropDown
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsManagerActiveBrandTest {

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

    @Test
    fun `default active brand is strange particle light`() {
        val manager = createManager()
        assertEquals("strangeparticle-light", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `user settings override default active brand`() {
        val manager = createManager(persistedDto = SettingsDto(activeBrand = "strangeparticle-dark"))
        assertEquals("strangeparticle-dark", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `user settings override environment variable for active brand`() {
        val manager = createManager(
            persistedDto = SettingsDto(activeBrand = "strangeparticle-light"),
            envVars = mapOf("SPRINGBOARD_ACTIVE_BRAND" to "strangeparticle-dark"),
        )
        assertEquals("strangeparticle-light", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.USER_SETTINGS, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `cli arg overrides environment variable for active brand`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_ACTIVE_BRAND" to "strangeparticle-light"),
            cliArgs = listOf("--active-brand", "strangeparticle-dark"),
        )
        assertEquals("strangeparticle-dark", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.PARAMS, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `unknown brand id from env var is ignored and default is used`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_ACTIVE_BRAND" to "bogus-brand"),
        )
        assertEquals("strangeparticle-light", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `unknown brand id from cli is ignored and default is used`() {
        val manager = createManager(
            cliArgs = listOf("--active-brand", "bogus-brand"),
        )
        assertEquals("strangeparticle-light", manager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `set user setting persists active brand through dto`() {
        val persistence = PersistenceServiceInMemoryFake()
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, persistence)
        manager.loadSettingsAtStartup()
        manager.setUserSetting(SettingsKey.ACTIVE_BRAND, "strangeparticle-dark")

        val reloaded = SettingsManager(RuntimeEnvironment.DesktopOsx, persistence)
        reloaded.loadSettingsAtStartup()
        assertEquals("strangeparticle-dark", reloaded.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND))
    }

    @Test
    fun `registry declaration exposes full options list with default id`() {
        val declaration = SettingsRegistry.require(SettingsKey.ACTIVE_BRAND).defaultValue as StringFromDropDown
        assertEquals("strangeparticle-light", declaration.defaultDropDownOptionId)
        assertEquals(2, declaration.dropDownOptions.size)
        kotlin.test.assertTrue(declaration.dropDownOptions.any { it.id == "strangeparticle-light" })
        kotlin.test.assertTrue(declaration.dropDownOptions.any { it.id == "strangeparticle-dark" })
    }
}
