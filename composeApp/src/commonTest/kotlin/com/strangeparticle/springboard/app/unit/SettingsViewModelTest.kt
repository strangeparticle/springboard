package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.*

class SettingsViewModelTest {

    private fun createViewModel(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
    ): SettingsViewModel {
        val settingsManager = createSettingsManagerForTest(target)
        return SettingsViewModel(settingsManager)
    }

    // -- clearAllUserSettings --

    @Test
    fun `clear all user settings restores defaults`() {
        val vm = createViewModel()
        vm.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        assertTrue(vm.getResolvedValue(SettingsKey.SURFACE_APPLESCRIPT_ERRORS) as Boolean)

        vm.clearAllUserSettings()
        assertFalse(vm.getResolvedValue(SettingsKey.SURFACE_APPLESCRIPT_ERRORS) as Boolean)
    }

    @Test
    fun `clear all user settings reveals params values`() {
        val settingsManager = createSettingsManagerForTest(
            target = RuntimeEnvironment.DesktopOsx,
            cliArgs = listOf("--surface-applescript-errors"),
        )
        val vm = SettingsViewModel(settingsManager)

        vm.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, false)
        assertFalse(vm.getResolvedValue(SettingsKey.SURFACE_APPLESCRIPT_ERRORS) as Boolean)

        vm.clearAllUserSettings()
        assertTrue(vm.getResolvedValue(SettingsKey.SURFACE_APPLESCRIPT_ERRORS) as Boolean)
    }

    @Test
    fun `active settings entries contain layer details`() {
        val settingsManager = createSettingsManagerForTest(
            target = RuntimeEnvironment.DesktopOsx,
            envVars = mapOf("SPRINGBOARD_SURFACE_APPLESCRIPT_ERRORS" to "true"),
        )
        val vm = SettingsViewModel(settingsManager)

        val entry = vm.activeSettingsEntries.first { it.displayName == "Surface AppleScript errors" }
        assertTrue(entry.layerDetails.contains("Env var: true"))
        assertTrue(entry.layerDetails.contains("Default:"))
    }

    // -- groupedSettings --

    @Test
    fun `grouped settings on desktop osx has general and desktop groups`() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val groups = vm.groupedSettings
        assertTrue(groups.size >= 2, "Desktop macOS should have at least general and desktop groups")

        val groupNames = groups.map { it.name }
        assertTrue("General" in groupNames)
        assertTrue("Desktop macOS" in groupNames)
    }

    @Test
    fun `grouped settings on wasm has only general group`() {
        val vm = createViewModel(target = RuntimeEnvironment.WASM)
        val groups = vm.groupedSettings
        assertEquals(1, groups.size)
        assertEquals("General", groups.first().name)

        for (item in groups.first().settings) {
            assertFalse(item is SettingItem.Desktop, "${item.key} should not appear on WASM")
        }
    }

    @Test
    fun `general group does not contain desktop settings`() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val generalGroup = vm.groupedSettings.find { it.name == "General" }
        assertNotNull(generalGroup)
        for (item in generalGroup.settings) {
            assertFalse(item is SettingItem.Desktop, "${item.key} is desktop-specific but appeared in General group")
        }
    }

    // -- settingsVersion --

    @Test
    fun `settings version increments on change`() {
        val vm = createViewModel()
        val initialVersion = vm.settingsVersion
        vm.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        assertEquals(initialVersion + 1, vm.settingsVersion)
    }
}
