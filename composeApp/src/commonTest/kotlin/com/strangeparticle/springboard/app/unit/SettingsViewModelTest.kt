package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.settings.*
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.*

class SettingsViewModelTest {

    private fun createViewModel(
        target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx,
        currentFilePath: () -> String? = { null },
    ): SettingsViewModel {
        val settingsManager = createTestSettingsManager(target)
        return SettingsViewModel(settingsManager, currentFilePath)
    }

    // -- designateCurrentFileAsStartup --

    @Test
    fun testDesignateCurrentFileAsStartupSucceeds() {
        val vm = createViewModel(currentFilePath = { "/path/to/board.json" })
        val result = vm.designateCurrentFileAsStartup()
        assertTrue(result)
        assertEquals(FilePath("/path/to/board.json"), vm.getResolvedValue(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun testDesignateCurrentFileAsStartupFailsWhenNoFileOpen() {
        val vm = createViewModel(currentFilePath = { null })
        val result = vm.designateCurrentFileAsStartup()
        assertFalse(result)
        assertNull(vm.getResolvedValue(SettingsKey.STARTUP_SPRINGBOARD))
    }

    @Test
    fun testClearStartupSpringboard() {
        val vm = createViewModel(currentFilePath = { "/path/to/board.json" })
        vm.designateCurrentFileAsStartup()
        assertNotNull(vm.getResolvedValue(SettingsKey.STARTUP_SPRINGBOARD))

        vm.clearStartupSpringboard()
        assertNull(vm.getResolvedValue(SettingsKey.STARTUP_SPRINGBOARD))
    }

    // -- groupedSettings --

    @Test
    fun testGroupedSettingsOnDesktopOsxHasGeneralAndDesktopGroups() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val groups = vm.groupedSettings
        assertTrue(groups.size >= 2, "Desktop macOS should have at least general and desktop groups")

        val groupNames = groups.map { it.name }
        assertTrue("General" in groupNames)
        assertTrue("Desktop macOS" in groupNames)
    }

    @Test
    fun testGroupedSettingsOnWasmHasOnlyGeneralGroup() {
        val vm = createViewModel(target = RuntimeEnvironment.WASM)
        val groups = vm.groupedSettings
        assertEquals(1, groups.size)
        assertEquals("General", groups.first().name)

        for (item in groups.first().settings) {
            assertFalse(item is SettingItem.Desktop, "${item.key} should not appear on WASM")
        }
    }

    @Test
    fun testGeneralGroupDoesNotContainDesktopSettings() {
        val vm = createViewModel(target = RuntimeEnvironment.DesktopOsx)
        val generalGroup = vm.groupedSettings.find { it.name == "General" }
        assertNotNull(generalGroup)
        for (item in generalGroup.settings) {
            assertFalse(item is SettingItem.Desktop, "${item.key} is desktop-specific but appeared in General group")
        }
    }

    // -- settingsVersion --

    @Test
    fun testSettingsVersionIncrementsOnChange() {
        val vm = createViewModel()
        val initialVersion = vm.settingsVersion
        vm.setUserSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, true)
        assertEquals(initialVersion + 1, vm.settingsVersion)
    }
}
