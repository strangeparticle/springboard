package com.strangeparticle.luther.session

import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.luther.client.provider.AiProvider
import com.strangeparticle.luther.client.provider.ChatRequest
import com.strangeparticle.luther.client.provider.ChatResponse
import com.strangeparticle.luther.client.provider.Model
import com.strangeparticle.luther.client.provider.ProviderConfig
import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private data class TestProviderConfig(val key: String) : ProviderConfig

private fun testProvider(clientFactoryCount: IntArray) = object : AiProvider {
    override val id = "p"
    override val displayName = "P"
    override fun isConfigured(config: ProviderConfig) = (config as TestProviderConfig).key.isNotBlank()
    override fun createClient(config: ProviderConfig, httpClient: HttpClient): AiProviderClient {
        clientFactoryCount[0]++
        return object : AiProviderClient {
            override suspend fun sendAiRequest(request: ChatRequest): ChatResponse =
                throw UnsupportedOperationException()
            override suspend fun listModels(): List<Model> = emptyList()
        }
    }
    override fun orderModelsForPicker(models: List<Model>) = models
}

private val NoopExecutionContextFactory = object : AiSessionToolCallExecutionContextFactory {
    override fun createToolCallExecutionContext(
        onStateChanged: () -> Unit,
        awaitUserApproval: suspend (toolCallId: String) -> Boolean,
    ): ToolCallExecutionContext = object : ToolCallExecutionContext {}
}

private val NoopSnapshotProvider = object : AiSessionSnapshotProvider {
    override fun getSnapshotJson(): String = "{}"
}

class LutherSessionTest {
    private fun session(clientFactoryCount: IntArray) = createLutherSession(
        providers = listOf(testProvider(clientFactoryCount)),
        settings = LutherSettings("p", "m1", TestProviderConfig("k")),
        httpClient = HttpClient(),
        toolHandlers = emptyList<ToolCallHandler>(),
        executionContextFactory = NoopExecutionContextFactory,
        snapshotProvider = NoopSnapshotProvider,
        systemPromptProvider = { "" },
    )

    @Test fun factory_rejectsIncompleteSettings() {
        assertFailsWith<IllegalArgumentException> {
            createLutherSession(
                providers = listOf(testProvider(IntArray(1))),
                settings = LutherSettings("p", "", TestProviderConfig("")),
                httpClient = HttpClient(),
                toolHandlers = emptyList(),
                executionContextFactory = NoopExecutionContextFactory,
                snapshotProvider = NoopSnapshotProvider,
                systemPromptProvider = { "" },
            )
        }
    }

    @Test fun modelOnlyUpdate_doesNotRebuildClient() {
        val clientFactoryCount = IntArray(1)
        val session = session(clientFactoryCount)
        val before = clientFactoryCount[0]
        session.updateConfiguration(LutherSettings("p", "m2", TestProviderConfig("k")))
        assertEquals(before, clientFactoryCount[0])
        assertEquals("m2", session.status.value.modelId)
        session.close()
    }

    @Test fun configChange_rebuildsClient_keepsHistoryReference() {
        val clientFactoryCount = IntArray(1)
        val session = session(clientFactoryCount)
        val historyRef = session.chatHistory
        session.updateConfiguration(LutherSettings("p", "m1", TestProviderConfig("k2")))
        assertTrue(clientFactoryCount[0] >= 2)
        assertTrue(session.chatHistory === historyRef)
        session.close()
    }
}
