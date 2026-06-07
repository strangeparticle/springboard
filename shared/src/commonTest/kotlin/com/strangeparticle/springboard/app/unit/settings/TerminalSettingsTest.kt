package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.platform.PreferredTerminal
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.items.core.OpenTerminalInNewWindowSetting
import com.strangeparticle.springboard.app.settings.items.core.PreferredTerminalSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalSettingsTest {

    @Test
    fun `preferred terminal defaults to Terminal app`() {
        assertEquals(PreferredTerminal.TerminalApp.id, PreferredTerminalSetting.defaultValue)
    }

    @Test
    fun `preferred terminal offers Terminal and iTerm options`() {
        val optionIds = PreferredTerminalSetting.options.map { it.id }
        assertEquals(
            listOf(PreferredTerminal.TerminalApp.id, PreferredTerminal.ITerm.id),
            optionIds,
        )
    }

    @Test
    fun `open terminal in new window defaults to off`() {
        assertFalse(OpenTerminalInNewWindowSetting.defaultValue)
    }

    @Test
    fun `both terminal settings are registered as core settings`() {
        val registry = SettingsRegistry(coreSettingsItems())
        assertNotNull(registry.byId(PreferredTerminalSetting.id))
        assertNotNull(registry.byId(OpenTerminalInNewWindowSetting.id))
    }

    @Test
    fun `preferred terminal enum ids are stable`() {
        assertEquals("terminal_app", PreferredTerminal.TerminalApp.id)
        assertEquals("iterm", PreferredTerminal.ITerm.id)
        assertTrue(PreferredTerminal.fromId("iterm") == PreferredTerminal.ITerm)
    }
}
