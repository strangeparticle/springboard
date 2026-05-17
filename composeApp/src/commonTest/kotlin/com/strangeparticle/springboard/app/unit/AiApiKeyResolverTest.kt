package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.ai.AiApiKeyResolver
import com.strangeparticle.springboard.app.settings.ai.AiProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AiApiKeyResolverTest {

    private fun createManager(
        persistedOpenAiKey: String? = null,
        persistedAnthropicKey: String? = null,
    ): SettingsManager {
        val manager = SettingsManager(RuntimeEnvironment.DesktopOsx, PersistenceServiceInMemoryFake())
        manager.loadSettingsAtStartup()
        if (persistedOpenAiKey != null) manager.setUserSetting(SettingsKey.AI_OPENAI_API_KEY, persistedOpenAiKey)
        if (persistedAnthropicKey != null) manager.setUserSetting(SettingsKey.AI_ANTHROPIC_API_KEY, persistedAnthropicKey)
        return manager
    }

    @Test
    fun `resolve returns null for AiProvider None`() {
        val resolver = AiApiKeyResolver(createManager(), environmentVariables = emptyMap())
        assertNull(resolver.resolve(AiProvider.None))
    }

    @Test
    fun `resolve returns env var when set for OpenAI`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedOpenAiKey = "persisted-key"),
            environmentVariables = mapOf("OPENAI_API_KEY" to "env-key"),
        )
        assertEquals("env-key", resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve returns env var when set for Anthropic`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedAnthropicKey = "persisted-key"),
            environmentVariables = mapOf("ANTHROPIC_API_KEY" to "env-key"),
        )
        assertEquals("env-key", resolver.resolve(AiProvider.Anthropic))
    }

    @Test
    fun `resolve falls back to persisted value when env var is unset`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedOpenAiKey = "persisted-key"),
            environmentVariables = emptyMap(),
        )
        assertEquals("persisted-key", resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve treats empty env var as unset and falls back to persisted`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedOpenAiKey = "persisted-key"),
            environmentVariables = mapOf("OPENAI_API_KEY" to ""),
        )
        assertEquals("persisted-key", resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve treats blank env var as unset and falls back to persisted`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedOpenAiKey = "persisted-key"),
            environmentVariables = mapOf("OPENAI_API_KEY" to "   "),
        )
        assertEquals("persisted-key", resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve returns null when env var is empty and no persisted value`() {
        val resolver = AiApiKeyResolver(
            createManager(),
            environmentVariables = mapOf("OPENAI_API_KEY" to ""),
        )
        assertNull(resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve returns null when persisted value is blank`() {
        val resolver = AiApiKeyResolver(
            createManager(persistedOpenAiKey = "   "),
            environmentVariables = emptyMap(),
        )
        assertNull(resolver.resolve(AiProvider.OpenAi))
    }

    @Test
    fun `resolve returns null when neither env var nor persisted value is set`() {
        val resolver = AiApiKeyResolver(createManager(), environmentVariables = emptyMap())
        assertNull(resolver.resolve(AiProvider.OpenAi))
        assertNull(resolver.resolve(AiProvider.Anthropic))
    }

    @Test
    fun `OpenAI env var does not satisfy Anthropic lookup`() {
        val resolver = AiApiKeyResolver(
            createManager(),
            environmentVariables = mapOf("OPENAI_API_KEY" to "env-key"),
        )
        assertNull(resolver.resolve(AiProvider.Anthropic))
    }
}
