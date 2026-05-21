package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.core.SurfaceAppleScriptErrorsSetting
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.shared.stubHttpClientForTests
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private fun createViewModel(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
    ): SettingsViewModel {
        val settingsManager = createSettingsManagerForTest(target)
        return SettingsViewModel(settingsManager, stubHttpClientForTests())
    }

    // -- clearAllUserSettings --

    @Test
    fun `clear all user settings restores defaults`() {
        val vm = createViewModel()
        vm.setUserSetting(SurfaceAppleScriptErrorsSetting, true)
        assertTrue(vm.getResolvedValue(SurfaceAppleScriptErrorsSetting))

        vm.clearAllUserSettings()
        assertFalse(vm.getResolvedValue(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `clear all user settings reveals params values`() {
        val settingsManager = createSettingsManagerForTest(
            target = RuntimeEnvironment.DesktopOsx,
            cliArgs = listOf("--surface-applescript-errors"),
        )
        val vm = SettingsViewModel(settingsManager, stubHttpClientForTests())

        vm.setUserSetting(SurfaceAppleScriptErrorsSetting, false)
        assertFalse(vm.getResolvedValue(SurfaceAppleScriptErrorsSetting))

        vm.clearAllUserSettings()
        assertTrue(vm.getResolvedValue(SurfaceAppleScriptErrorsSetting))
    }

    @Test
    fun `active settings entries contain layer details`() {
        val settingsManager = createSettingsManagerForTest(
            target = RuntimeEnvironment.DesktopOsx,
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        val vm = SettingsViewModel(settingsManager, stubHttpClientForTests())

        val entry = vm.activeSettingsEntries.first { it.displayName == "Surface AppleScript errors" }
        assertTrue(entry.layerDetails.contains("Env var: true"))
        assertTrue(entry.layerDetails.contains("Default:"))
    }

    // -- groupedSettings --

    @Test
    fun `grouped settings on desktop osx has general and desktop groups`() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val groups = vm.groupedSettings
        val groupKinds = groups.map { it.group }
        assertTrue(SettingsGroup.General in groupKinds)
        assertTrue(SettingsGroup.DesktopMacOS in groupKinds)
    }

    @Test
    fun `grouped settings on wasm has no desktop group`() {
        val vm = createViewModel(target = RuntimeEnvironment.WASM)
        val groups = vm.groupedSettings
        val groupKinds = groups.map { it.group }
        assertTrue(SettingsGroup.General in groupKinds)
        assertFalse(SettingsGroup.DesktopMacOS in groupKinds, "WASM should not show the Desktop group")
    }

    @Test
    fun `general group does not contain desktop-only settings`() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val generalGroup = vm.groupedSettings.find { it.group == SettingsGroup.General }
        assertNotNull(generalGroup)
        for (item in generalGroup.items) {
            assertEquals(SettingsGroup.General, item.group, "${item.id} is not in the General group")
        }
    }

    // -- settingsVersion --

    @Test
    fun `settings version increments on change`() {
        val vm = createViewModel()
        val initialVersion = vm.settingsVersion
        vm.setUserSetting(SurfaceAppleScriptErrorsSetting, true)
        assertEquals(initialVersion + 1, vm.settingsVersion)
    }
}
