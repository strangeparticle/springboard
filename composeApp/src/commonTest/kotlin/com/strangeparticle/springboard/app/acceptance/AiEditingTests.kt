package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.test.ExperimentalTestApi
import com.strangeparticle.editio.client.AiClientErrorType
import com.strangeparticle.editio.client.AiClientException
import com.strangeparticle.editio.session.AiSessionManager
import com.strangeparticle.editio.session.AiSessionSnapshotProvider
import com.strangeparticle.editio.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.toolcall.ToolCall
import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallRegistry
import com.strangeparticle.springboard.app.editio.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.editio.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.editio.SystemPromptBuilder
import com.strangeparticle.springboard.app.editio.toolcall.AddAppToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddEnvironmentToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddResourceToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.AddUrlActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.CreateSpringboardToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.RemoveActivatorToolCallHandler
import com.strangeparticle.springboard.app.editio.toolcall.SaveSpringboardToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.shared.AiClientInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class AiEditingTests {

    @Test
    fun `fake provider can add an activator through tools`() = runTest {
        val fixture = createFixture()
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall("call-resource", "add_resource", args("tab_id" to tabId, "id" to "res2", "name" to "Logs")),
                ToolCall("call-activator", "add_url_activator", args("tab_id" to tabId, "app_id" to "app1", "resource_id" to "res2", "environment_id" to "dev", "url" to "https://logs.example.com")),
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Added logs.")

        fixture.manager.submit("Add logs activator").join()

        val springboard = fixture.viewModel.springboard
        assertNotNull(springboard)
        assertTrue(springboard.activators.any { it.resourceId == "res2" })
    }

    @Test
    fun `fake provider executes multi tool turn sequentially`() = runTest {
        val fixture = createFixture()
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall("call-resource", "add_resource", args("tab_id" to tabId, "id" to "res2", "name" to "Logs")),
                ToolCall("call-activator", "add_url_activator", args("tab_id" to tabId, "app_id" to "app1", "resource_id" to "res2", "environment_id" to "dev", "url" to "https://logs.example.com")),
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Done.")

        fixture.manager.submit("Add resource and activator").join()

        assertEquals(2, fixture.manager.history.filterIsInstance<com.strangeparticle.editio.toolcall.ToolCallProviderClientMessage>().size)
    }

    @Test
    fun `fake provider can undo by issuing inverse tool call`() = runTest {
        val fixture = createFixture()
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(ToolCall("call-remove", "remove_activator", args("tab_id" to tabId, "app_id" to "app1", "resource_id" to "res1", "environment_id" to "dev")))
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Removed it.")

        fixture.manager.submit("Undo the existing activator").join()

        assertTrue(fixture.viewModel.springboard?.activators.orEmpty().isEmpty())
    }

    @Test
    fun `save springboard writes only after approval`() = runTest {
        val fixture = createFixture(source = "/tmp/test-springboard.json")
        val tabId = fixture.viewModel.activeTabId
        fixture.viewModel.markTabDirty(tabId)
        fixture.registry.register(SaveSpringboardToolCallHandler())
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(ToolCall("call-save", "save_springboard", args("tab_id" to tabId)))
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Saved.")

        val job = fixture.manager.submit("Save it")
        runCurrent()
        assertNull(fixture.fileService.writtenFiles["/tmp/test-springboard.json"])

        fixture.manager.onApprovalDecision("call-save", true)
        job.join()

        assertNotNull(fixture.fileService.writtenFiles["/tmp/test-springboard.json"])
    }

    @Test
    fun `fake provider can create and populate a new springboard`() = runTest {
        val fixture = createFixture()
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(ToolCall("call-create", "create_springboard", args()))
        )
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall("call-app", "add_app", args("tab_id" to "tab-2", "id" to "metrics", "name" to "Metrics")),
                ToolCall("call-resource", "add_resource", args("tab_id" to "tab-2", "id" to "dashboard", "name" to "Dashboard")),
                ToolCall("call-env", "add_environment", args("tab_id" to "tab-2", "id" to "prod", "name" to "Production")),
                ToolCall("call-activator", "add_url_activator", args("tab_id" to "tab-2", "app_id" to "metrics", "resource_id" to "dashboard", "environment_id" to "prod", "url" to "https://metrics.example.com")),
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Created a new springboard.")

        fixture.manager.submit("Create a new springboard for metrics").join()

        val activeTab = fixture.viewModel.activeTab
        assertNotNull(activeTab)
        assertEquals("Untitled-1", activeTab.springboard?.name)
        assertNull(activeTab.source)
        assertTrue(activeTab.isDirty)
        assertEquals("", activeTab.springboard?.jsonSource)
        val springboard = activeTab.springboard
        assertNotNull(springboard)
        assertTrue(springboard.apps.any { it.id == "metrics" })
        assertTrue(springboard.resources.any { it.id == "dashboard" })
        assertTrue(springboard.environments.any { it.id == "prod" })
        assertTrue(springboard.activators.any { it.appId == "metrics" && it.resourceId == "dashboard" && it.environmentId == "prod" })
    }

    @Test
    fun `provider error renders chat error and next submit can recover`() = runTest {
        val fixture = createFixture()
        fixture.aiClient.sendAiRequestException = AiClientException(AiClientErrorType.Network, "network unavailable")

        fixture.manager.submit("Try").join()

        assertEquals(ChatMessagePart.ChatError("network unavailable"), fixture.manager.transcriptParts.last())
        fixture.aiClient.sendAiRequestException = null
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Recovered.")
        fixture.manager.submit("Try again").join()
        assertEquals(ChatMessagePart.AssistantText("Recovered."), fixture.manager.transcriptParts.last())
    }

    private fun TestScope.createFixture(source: String = "/test/springboard.json"): Fixture {
        val settingsManager = SettingsManager(RuntimeEnvironment.DesktopOsx, PersistenceServiceInMemoryFake())
        settingsManager.loadSettingsAtStartup()
        val fileService = PlatformFileContentServiceInMemoryFake()
        val viewModel = SpringboardViewModel(
            settingsManager = settingsManager,
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
            fileContentService = fileService,
        )
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, source)
        val aiClient = AiClientInMemoryFake()
        val registry = ToolCallRegistry().apply {
            register(AddAppToolCallHandler())
            register(AddEnvironmentToolCallHandler())
            register(AddResourceToolCallHandler())
            register(AddUrlActivatorToolCallHandler())
            register(CreateSpringboardToolCallHandler())
            register(RemoveActivatorToolCallHandler())
        }
        val manager = AiSessionManager(
            aiClient = aiClient,
            toolCallRegistry = registry,
            snapshotProvider = object : AiSessionSnapshotProvider {
                override fun getSnapshotJson(): String = SpringboardAppSnapshot.capture(viewModel).toCompactJson()
            },
            toolCallExecutionContextFactory = object : AiSessionToolCallExecutionContextFactory {
                override fun createToolCallExecutionContext(
                    onStateChanged: () -> Unit,
                    awaitUserApproval: suspend (toolCallId: String) -> Boolean,
                ): ToolCallExecutionContext = object : SpringboardToolCallExecutionContext {
                    override val viewModel: SpringboardViewModel = viewModel
                    override fun markStateChanged() = onStateChanged()
                    override suspend fun awaitUserApproval(toolCallId: String): Boolean = awaitUserApproval(toolCallId)
                }
            },
            systemPromptProvider = { SystemPromptBuilder.build() },
            modelIdProvider = { "fake-model" },
            coroutineScope = this,
        )
        return Fixture(viewModel, fileService, aiClient, registry, manager)
    }

    private fun args(vararg pairs: Pair<String, String>): String = buildJsonObject {
        pairs.forEach { (key, value) -> put(key, value) }
        put("display_message", "done")
    }.toString()

    private data class Fixture(
        val viewModel: SpringboardViewModel,
        val fileService: PlatformFileContentServiceInMemoryFake,
        val aiClient: AiClientInMemoryFake,
        val registry: ToolCallRegistry,
        val manager: AiSessionManager,
    )
}
