package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.items.core.HideAppAfterActivationSetting
import com.strangeparticle.springboard.app.settings.items.core.OpenUrlsInNewWindowSingleSetting
import com.strangeparticle.springboard.app.settings.items.core.ResetKeyNavAfterKeyNavActivationSetting
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import com.strangeparticle.springboard.app.settings.items.core.SurfaceAppleScriptErrorsSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsManagerTest {

    private fun createManager(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        persistedDto: SettingsDto? = null,
        envVars: Map<String, String> = emptyMap(),
        cliArgs: List<String> = emptyList(),
        urlParams: Map<String, String> = emptyMap(),
    ): SettingsManager {
        val persistence = PersistenceServiceInMemoryFake()
        if (persistedDto != null) persistence.persistSettings(persistedDto)
        val registry = SettingsRegistry(
            coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() }
        )
        val manager = SettingsManager(target, registry, persistence)
        manager.loadSettingsAtStartup(envVars, cliArgs, urlParams)
        return manager
    }

    @Test
    fun `default values resolve from item defaults`() {
        val manager = createManager()
        assertTrue(manager.resolveValue(OpenUrlsInNewWindowSingleSetting))
        assertFalse(manager.resolveValue(SurfaceAppleScriptErrorsSetting))
        assertTrue(manager.resolveValue(ResetKeyNavAfterKeyNavActivationSetting))
        assertEquals(emptyList(), manager.resolveValue(StartupTabsSetting))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `persisted user settings override defaults`() {
        val manager = createManager(
            persistedDto = SettingsDto(values = mapOf(
                SurfaceAppleScriptErrorsSetting.id to JsonPrimitive(true),
            )),
        )
        assertTrue(manager.resolveValue(SurfaceAppleScriptErrorsSetting))
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, manager.getSource(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `persisted startup tabs round-trip`() {
        val manager = createManager(
            persistedDto = SettingsDto(values = mapOf(
                StartupTabsSetting.id to JsonArray(listOf(JsonPrimitive("/path/to/file.json"))),
            )),
        )
        assertEquals(listOf("/path/to/file.json"), manager.resolveValue(StartupTabsSetting))
    }

    @Test
    fun `persisted user settings take precedence over env vars`() {
        val manager = createManager(
            persistedDto = SettingsDto(values = mapOf(
                SurfaceAppleScriptErrorsSetting.id to JsonPrimitive(false),
            )),
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertFalse(manager.resolveValue(SurfaceAppleScriptErrorsSetting))
        assertEquals(SettingsSource.USER_SETTINGS_FROM_PERSISTENCE, manager.getSource(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `env vars override defaults when no persisted user setting exists`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        assertTrue(manager.resolveValue(SurfaceAppleScriptErrorsSetting))
        assertEquals(SettingsSource.ENVIRONMENT_VARIABLE, manager.getSource(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `cli flags beat env vars`() {
        val manager = createManager(
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "false"),
            cliArgs = listOf("--surface-applescript-errors"),
        )
        assertTrue(manager.resolveValue(SurfaceAppleScriptErrorsSetting))
        assertEquals(SettingsSource.CLI_FLAG, manager.getSource(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `user setting mutation persists to in-memory layer`() {
        val manager = createManager()
        manager.setUserSetting(HideAppAfterActivationSetting, false)
        assertFalse(manager.resolveValue(HideAppAfterActivationSetting))
        assertEquals(SettingsSource.USER_SETTINGS_FROM_SESSION, manager.getSource(HideAppAfterActivationSetting))
    }

    @Test
    fun `non-applicable settings are filtered for the environment`() {
        val manager = createManager(target = RuntimeEnvironment.WASM)
        val items = manager.applicableSettings().map { it.id }
        // Desktop-only items should be filtered out
        assertFalse(SurfaceAppleScriptErrorsSetting.id in items)
        // General items survive
        assertTrue(ResetKeyNavAfterKeyNavActivationSetting.id in items)
    }
}
