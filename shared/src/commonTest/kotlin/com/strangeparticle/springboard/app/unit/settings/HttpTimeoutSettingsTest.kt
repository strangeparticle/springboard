package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.settings.items.core.HttpContentTimeoutSecondsSetting
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.shared.createSettingsRegistryForTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HttpTimeoutSettingsTest {

    @Test
    fun `timeout settings are registered core settings`() {
        val registry = createSettingsRegistryForTest()

        assertNotNull(registry.byId(HttpContentTimeoutSecondsSetting.id))
        assertNotNull(registry.byId(HttpAiProviderTimeoutSecondsSetting.id))
    }

    @Test
    fun `timeout settings resolve expected defaults`() {
        val manager = createSettingsManagerForTest()

        assertEquals(30, manager.resolveValue(HttpContentTimeoutSecondsSetting))
        assertEquals(180, manager.resolveValue(HttpAiProviderTimeoutSecondsSetting))
        assertEquals(SettingsSource.APP_DEFAULT, manager.getSource(HttpAiProviderTimeoutSecondsSetting))
    }

    @Test
    fun `timeout settings can be supplied from cli flags`() {
        val manager = createSettingsManagerForTest(
            cliArgs = listOf(
                "--http-content-timeout-seconds", "45",
                "--http-ai-provider-timeout-seconds", "300",
            ),
        )

        assertEquals(45, manager.resolveValue(HttpContentTimeoutSecondsSetting))
        assertEquals(300, manager.resolveValue(HttpAiProviderTimeoutSecondsSetting))
        assertEquals(SettingsSource.CLI_FLAG, manager.getSource(HttpAiProviderTimeoutSecondsSetting))
    }

    @Test
    fun `timeout settings expose expected derived external names`() {
        assertEquals("SPRINGBOARD_HTTP_CONTENT_TIMEOUT_SECONDS", SettingsKeyNaming.envVarName(HttpContentTimeoutSecondsSetting))
        assertEquals("SPRINGBOARD_HTTP_AI_PROVIDER_TIMEOUT_SECONDS", SettingsKeyNaming.envVarName(HttpAiProviderTimeoutSecondsSetting))
        assertEquals("--http-content-timeout-seconds", SettingsKeyNaming.cliFlag(HttpContentTimeoutSecondsSetting))
        assertEquals("--http-ai-provider-timeout-seconds", SettingsKeyNaming.cliFlag(HttpAiProviderTimeoutSecondsSetting))
    }
}
