package com.strangeparticle.luther.client.provider

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.AiProviderClientModelInfo
import com.strangeparticle.luther.client.AiProviderClientRequest
import com.strangeparticle.luther.client.AiProviderClientResponse
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class FakeConfig(val key: String) : ProviderConfig

private class FakeProvider(
    private val models: List<AiProviderClientModelInfo>,
) : AiProvider {
    override val id = "fake"
    override val displayName = "Fake"
    override fun isConfigured(config: ProviderConfig) = (config as FakeConfig).key.isNotBlank()
    override fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient =
        object : AiProviderClient {
            override suspend fun sendAiRequest(request: AiProviderClientRequest): AiProviderClientResponse =
                throw UnsupportedOperationException()
            override suspend fun listModels(): List<AiProviderClientModelInfo> = models
        }
    override fun orderModelsForPicker(models: List<AiProviderClientModelInfo>) =
        models.filter { it.supportsToolCalling }
}

class LutherProviderCatalogTest {
    @Test
    fun availableProviders_listsRegisteredProviders() {
        val catalog = LutherProviderCatalog(listOf(FakeProvider(emptyList())), httpClient = null)
        assertEquals(listOf(Choice("fake", "Fake")), catalog.availableProviders())
    }

    @Test
    fun availableModels_filtersAndMapsThroughProviderOrdering() = runTest {
        val models = listOf(
            AiProviderClientModelInfo("m1", "Model One", supportsToolCalling = true),
            AiProviderClientModelInfo("m2", null, supportsToolCalling = false),
        )
        val catalog = LutherProviderCatalog(listOf(FakeProvider(models)), httpClient = null)
        val result = catalog.availableModels("fake", FakeConfig("k"))
        assertEquals(listOf(Choice("m1", "Model One")), result)
    }
}
