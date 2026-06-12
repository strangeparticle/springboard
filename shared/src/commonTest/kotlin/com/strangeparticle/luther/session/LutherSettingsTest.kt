package com.strangeparticle.luther.session

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import com.strangeparticle.luther.client.AiProviderClientRequest
import com.strangeparticle.luther.client.AiProviderClientResponse
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.client.provider.ProviderConfig
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data class Cfg(val key: String) : ProviderConfig
private val provider = object : AiProvider {
    override val id = "p"; override val displayName = "P"
    override fun isConfigured(config: ProviderConfig) = (config as Cfg).key.isNotBlank()
    override fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient =
        object : AiProviderClient {
            override suspend fun sendAiRequest(request: AiProviderClientRequest): AiProviderClientResponse = throw UnsupportedOperationException()
            override suspend fun listModels(): List<AiProviderClientModelInfo> = emptyList()
        }
    override fun orderModelsForPicker(models: List<AiProviderClientModelInfo>) = models
}

class LutherSettingsTest {
    @Test fun complete_whenProviderConfiguredAndModelSet() {
        val s = LutherSettings("p", "m", Cfg("k"))
        assertTrue(s.isComplete(provider))
    }
    @Test fun incomplete_whenModelBlank() {
        assertFalse(LutherSettings("p", "", Cfg("k")).isComplete(provider))
    }
    @Test fun incomplete_whenProviderNotConfigured() {
        assertFalse(LutherSettings("p", "m", Cfg("")).isComplete(provider))
    }
}
