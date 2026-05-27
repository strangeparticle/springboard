package com.strangeparticle.springboard.app.acceptance

import androidx.compose.ui.test.ExperimentalTestApi
import com.strangeparticle.luther.client.AiProviderClientErrorType
import com.strangeparticle.luther.client.AiProviderClientException
import com.strangeparticle.luther.session.AiSessionManager
import com.strangeparticle.luther.session.AiSessionSnapshotProvider
import com.strangeparticle.luther.session.AiSessionToolCallExecutionContextFactory
import com.strangeparticle.luther.session.ChatMessagePart
import com.strangeparticle.luther.toolcall.ToolCall
import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallRegistry
import com.strangeparticle.springboard.app.luther.SpringboardAppSnapshot
import com.strangeparticle.springboard.app.luther.SpringboardToolCallExecutionContext
import com.strangeparticle.springboard.app.luther.SystemPromptBuilder
import com.strangeparticle.springboard.app.luther.toolcall.ActivateColumnToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ActivateCoordinateToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ActivateCoordinatesToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ActivateRowToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.AddAppToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.AddEnvironmentToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.AddResourceToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.AddUrlActivatorToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.CreateSpringboardToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.RemoveActivatorToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.SaveSpringboardToolCallHandler
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.shared.AiProviderClientInMemoryFake
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
internal class AiAssistantTests {

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

        val springboardUnfiltered = fixture.viewModel.springboardUnfiltered
        assertNotNull(springboardUnfiltered)
        assertTrue(springboardUnfiltered.activators.any { it.resourceId == "res2" })
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

        assertEquals(2, fixture.manager.history.filterIsInstance<com.strangeparticle.luther.toolcall.ToolCallProviderClientMessage>().size)
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

        assertTrue(fixture.viewModel.springboardUnfiltered?.activators.orEmpty().isEmpty())
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
        assertEquals("Untitled-1", activeTab.springboardFilteredForRuntime?.name)
        assertNull(activeTab.source)
        assertTrue(activeTab.isDirty)
        assertEquals("", activeTab.springboardUnfiltered?.jsonSource)
        val springboardUnfiltered = activeTab.springboardUnfiltered
        assertNotNull(springboardUnfiltered)
        assertTrue(springboardUnfiltered.apps.any { it.id == "metrics" })
        assertTrue(springboardUnfiltered.resources.any { it.id == "dashboard" })
        assertTrue(springboardUnfiltered.environments.any { it.id == "prod" })
        assertTrue(springboardUnfiltered.activators.any { it.appId == "metrics" && it.resourceId == "dashboard" && it.environmentId == "prod" })
    }

    @Test
    fun `activate_coordinate opens URL of URL activator`() = runTest {
        val fixture = createFixture()
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate",
                    "activate_coordinate",
                    args(
                        "tab_id" to tabId,
                        "environment_id" to "dev",
                        "app_id" to "app1",
                        "resource_id" to "res1",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Opened.")

        fixture.manager.submit("Open the dashboard").join()

        assertEquals(listOf("https://example.com"), fixture.activationService.openedUrls)
    }

    @Test
    fun `activate_coordinate runs command of command activator`() = runTest {
        val fixture = createFixture(initialConfig = TestFixtureJson.COMMAND_ACTIVATOR)
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate",
                    "activate_coordinate",
                    args(
                        "tab_id" to tabId,
                        "environment_id" to "dev",
                        "app_id" to "app1",
                        "resource_id" to "res1",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Done.")

        fixture.manager.submit("Run the command").join()

        assertEquals(listOf("echo test"), fixture.activationService.executedCommands)
        assertTrue(fixture.activationService.openedUrls.isEmpty())
    }

    @Test
    fun `activate_coordinate returns no_activators_resolved when coordinate has no activator`() = runTest {
        val fixture = createFixture()
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate",
                    "activate_coordinate",
                    args(
                        "tab_id" to tabId,
                        "environment_id" to "dev",
                        "app_id" to "app1",
                        "resource_id" to "nonexistent",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Nothing to open.")

        fixture.manager.submit("Open the missing thing").join()

        assertTrue(fixture.activationService.openedUrls.isEmpty())
        val lastToolMessage = fixture.manager.history
            .filterIsInstance<com.strangeparticle.luther.toolcall.ToolCallProviderClientMessage>()
            .last()
        assertTrue(lastToolMessage.content.contains("no_activators_resolved"))
    }

    @Test
    fun `activate_row opens every URL in row using all-envs fallback`() = runTest {
        val rowFixtureJson = """
        {
          "name": "Row Test",
          "environments": [{ "id": "prod", "name": "Prod" }],
          "apps": [
            { "id": "app1", "name": "App One" },
            { "id": "app2", "name": "App Two" }
          ],
          "resources": [{ "id": "res1", "name": "Dashboard" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://prod.example.com/app1" },
            { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "ALL", "url": "https://all.example.com/app2" }
          ]
        }
        """.trimIndent()
        val fixture = createFixture(initialConfig = rowFixtureJson)
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate-row",
                    "activate_row",
                    args(
                        "tab_id" to tabId,
                        "environment_id" to "prod",
                        "resource_id" to "res1",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Opened row.")

        fixture.manager.submit("Open the row").join()

        // app1 has a prod-specific URL, app2 falls back to its ALL-env activator.
        assertEquals(
            listOf(
                "https://prod.example.com/app1",
                "https://all.example.com/app2",
            ),
            fixture.activationService.openedUrls,
        )
    }

    @Test
    fun `activate_column opens every URL in column using all-envs fallback`() = runTest {
        val fixture = createFixture(initialConfig = TestFixtureJson.COMMAND_STRICT_WITH_ALL_ENVS_URL_FALLBACK)
        val tabId = fixture.viewModel.activeTabId
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate-column",
                    "activate_column",
                    args(
                        "tab_id" to tabId,
                        "environment_id" to "prod",
                        "app_id" to "app1",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Opened column.")

        fixture.manager.submit("Open the column").join()

        // The prod (app1, res1) cell has a command, not a URL; that command should run.
        assertEquals(listOf("echo prod"), fixture.activationService.executedCommands)
    }

    @Test
    fun `activate_coordinates batches multiple URLs in a single call`() = runTest {
        val fixture = createFixture(initialConfig = TestFixtureJson.MULTI_ENV_WITH_COMMON)
        val tabId = fixture.viewModel.activeTabId
        val argsJson = kotlinx.serialization.json.buildJsonObject {
            put("tab_id", tabId)
            put("coordinates", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("environment_id", "common")
                    put("app_id", "app1")
                    put("resource_id", "res1")
                })
                add(kotlinx.serialization.json.buildJsonObject {
                    put("environment_id", "common")
                    put("app_id", "app1")
                    put("resource_id", "res2")
                })
            })
        }.toString()
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(ToolCall("call-activate-many", "activate_coordinates", argsJson))
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Opened a batch.")

        fixture.manager.submit("Open these two").join()

        assertEquals(
            listOf(
                "https://example.com/common/app1/dash",
                "https://example.com/common/app1/logs",
            ),
            fixture.activationService.openedUrls,
        )
    }

    @Test
    fun `activation tools target non-active tab without changing active tab`() = runTest {
        val fixture = createFixture()
        val firstTabId = fixture.viewModel.activeTabId
        val secondTabId = fixture.viewModel.createTab()!!
        fixture.viewModel.loadConfig(TestFixtureJson.ALTERNATIVE_URL_ONLY, "/other.json")
        fixture.viewModel.selectTab(firstTabId)
        assertEquals(firstTabId, fixture.viewModel.activeTabId)

        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-cross",
                    "activate_coordinate",
                    args(
                        "tab_id" to secondTabId,
                        "environment_id" to "staging",
                        "app_id" to "web",
                        "resource_id" to "dashboard",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Opened.")

        fixture.manager.submit("Open the other tab's dashboard").join()

        assertEquals(listOf("https://alt.example.com"), fixture.activationService.openedUrls)
        assertEquals(firstTabId, fixture.viewModel.activeTabId)
    }

    @Test
    fun `activate_coordinate returns missing_tab for unknown tab id`() = runTest {
        val fixture = createFixture()
        fixture.aiClient.responseQueue += fixture.aiClient.multipleToolCalls(
            listOf(
                ToolCall(
                    "call-activate",
                    "activate_coordinate",
                    args(
                        "tab_id" to "no-such-tab",
                        "environment_id" to "dev",
                        "app_id" to "app1",
                        "resource_id" to "res1",
                    ),
                )
            )
        )
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Could not find tab.")

        fixture.manager.submit("Open on missing tab").join()

        assertTrue(fixture.activationService.openedUrls.isEmpty())
        val lastToolMessage = fixture.manager.history
            .filterIsInstance<com.strangeparticle.luther.toolcall.ToolCallProviderClientMessage>()
            .last()
        assertTrue(lastToolMessage.content.contains("missing_tab"))
    }

    @Test
    fun `provider error renders chat error and next submit can recover`() = runTest {
        val fixture = createFixture()
        fixture.aiClient.sendAiRequestException = AiProviderClientException(AiProviderClientErrorType.Network, "network unavailable")

        fixture.manager.submit("Try").join()

        assertEquals(ChatMessagePart.ChatError("network unavailable"), fixture.manager.transcriptParts.last())
        fixture.aiClient.sendAiRequestException = null
        fixture.aiClient.responseQueue += fixture.aiClient.textOnly("Recovered.")
        fixture.manager.submit("Try again").join()
        assertEquals(ChatMessagePart.AssistantText("Recovered."), fixture.manager.transcriptParts.last())
    }

    private fun TestScope.createFixture(
        source: String = "/test/springboard.json",
        initialConfig: String = TestFixtureJson.URL_ONLY,
    ): Fixture {
        val settingsManager = SettingsManager(RuntimeEnvironment.DesktopOsx, com.strangeparticle.springboard.app.shared.createSettingsRegistryForTest(), PersistenceServiceInMemoryFake())
        settingsManager.loadSettingsAtStartup()
        val fileService = PlatformFileContentServiceInMemoryFake()
        val activationService = PlatformActivationServiceInMemoryFake()
        val viewModel = SpringboardViewModel(
            settingsManager = settingsManager,
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = activationService,
            fileContentService = fileService,
        )
        viewModel.loadConfig(initialConfig, source)
        val aiClient = AiProviderClientInMemoryFake()
        val registry = ToolCallRegistry().apply {
            register(ActivateColumnToolCallHandler())
            register(ActivateCoordinateToolCallHandler())
            register(ActivateCoordinatesToolCallHandler())
            register(ActivateRowToolCallHandler())
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
        return Fixture(viewModel, fileService, activationService, aiClient, registry, manager)
    }

    private fun args(vararg pairs: Pair<String, String>): String = buildJsonObject {
        pairs.forEach { (key, value) -> put(key, value) }
        put("display_message", "done")
    }.toString()

    private data class Fixture(
        val viewModel: SpringboardViewModel,
        val fileService: PlatformFileContentServiceInMemoryFake,
        val activationService: PlatformActivationServiceInMemoryFake,
        val aiClient: AiProviderClientInMemoryFake,
        val registry: ToolCallRegistry,
        val manager: AiSessionManager,
    )
}
