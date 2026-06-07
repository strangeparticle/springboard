package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.luther.client.provider.openai.OpenAiApiKeySetting
import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
import com.strangeparticle.springboard.app.settings.items.core.ActiveBrandSetting
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsKeyNamingTest {

    @Test
    fun `derives SPRINGBOARD_ envvar from id when no override`() {
        assertEquals("SPRINGBOARD_ACTIVE_BRAND", SettingsKeyNaming.envVarName(ActiveBrandSetting))
        assertEquals("SPRINGBOARD_STARTUP_TABS", SettingsKeyNaming.envVarName(StartupTabsSetting))
    }

    @Test
    fun `uses envvar override when item provides one`() {
        assertEquals("OPENAI_API_KEY", SettingsKeyNaming.envVarName(OpenAiApiKeySetting))
    }

    @Test
    fun `url param name lowercases and hyphenates the id`() {
        assertEquals("active-brand", SettingsKeyNaming.urlParamName(ActiveBrandSetting))
        assertEquals("ai-openai-api-key", SettingsKeyNaming.urlParamName(OpenAiApiKeySetting))
    }

    @Test
    fun `cli flag prefixes the url param name with two dashes`() {
        assertEquals("--active-brand", SettingsKeyNaming.cliFlag(ActiveBrandSetting))
    }

    @Test
    fun `json key is the id verbatim`() {
        assertEquals("active_brand", SettingsKeyNaming.jsonKey(ActiveBrandSetting))
        assertEquals("ai.openai.api_key", SettingsKeyNaming.jsonKey(OpenAiApiKeySetting))
    }
}
