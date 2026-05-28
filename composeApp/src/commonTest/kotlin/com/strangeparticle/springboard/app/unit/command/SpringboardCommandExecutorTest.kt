package com.strangeparticle.springboard.app.unit.command

import com.strangeparticle.springboard.app.command.SpringboardCommandExecutorDefaultImpl
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoader
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class SpringboardCommandExecutorTest {
    private val springboardJson = """
    {
      "name": "Test",
      "environments": [
        { "id": "dev", "name": "Dev" }
      ],
      "apps": [
        { "id": "github", "name": "GitHub" }
      ],
      "resources": [
        { "id": "repo", "name": "Repository" }
      ],
      "activators": [
        { "type": "url", "appId": "github", "resourceId": "repo", "environmentId": "dev", "url": "https://github.example/repo" }
      ]
    }
    """.trimIndent()

    private data class Fixture(
        val viewModel: SpringboardViewModel,
        val activationService: PlatformActivationServiceInMemoryFake,
        val executor: SpringboardCommandExecutorDefaultImpl,
    )

    private fun fixture(contentLoader: SpringboardContentLoader? = null): Fixture {
        val activationService = PlatformActivationServiceInMemoryFake()
        val viewModel = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
            contentLoader = contentLoader,
        )
        return Fixture(
            viewModel = viewModel,
            activationService = activationService,
            executor = SpringboardCommandExecutorDefaultImpl(viewModel),
        )
    }

    @Test
    fun `status reports active tab and tab count`() = runTest {
        val (viewModel, _, executor) = fixture()
        viewModel.loadConfig(springboardJson, "/tmp/test.springboard.json")

        val result = executor.execute(SpringboardCommand.Status)

        val success = assertIs<SpringboardCommandResult.Success>(result)
        val data = success.data!!.jsonObject
        assertEquals("true", data.getValue("running").jsonPrimitive.content)
        assertEquals(viewModel.activeTabId, data.getValue("activeTabId").jsonPrimitive.content)
        assertEquals("1", data.getValue("loadedTabCount").jsonPrimitive.content)
    }

    @Test
    fun `activate coordinate executes active tab activator`() = runTest {
        val (viewModel, activationService, executor) = fixture()
        viewModel.loadConfig(springboardJson, "/tmp/test.springboard.json")

        val result = executor.execute(
            SpringboardCommand.ActivateCoordinate(
                environmentId = "dev",
                appId = "github",
                resourceId = "repo",
            )
        )

        val success = assertIs<SpringboardCommandResult.Success>(result)
        assertEquals("Activated 1 activator.", success.message)
        assertEquals(listOf("https://github.example/repo"), activationService.openedUrls)
    }

    @Test
    fun `activate coordinate returns coordinate not found when no activator resolves`() = runTest {
        val (_, activationService, executor) = fixture()

        val result = executor.execute(
            SpringboardCommand.ActivateCoordinate(
                environmentId = "dev",
                appId = "github",
                resourceId = "repo",
            )
        )

        val failure = assertIs<SpringboardCommandResult.Failure>(result)
        assertEquals(SpringboardCommandErrorCode.SpringboardNotLoaded, failure.code)
        assertEquals(emptyList(), activationService.openedUrls)
    }

    @Test
    fun `open springboard loads source through configured content loader`() = runTest {
        val source = "/tmp/open.springboard.json"
        val (viewModel, _, executor) = fixture(
            contentLoader = MapContentLoader(mapOf(source to springboardJson))
        )

        val result = executor.execute(
            SpringboardCommand.OpenSpringboard(
                source = source,
                inNewTab = false,
            )
        )

        val success = assertIs<SpringboardCommandResult.Success>(result)
        assertEquals("Opened Test.", success.message)
        assertEquals(source, viewModel.activeTab?.source)
    }

    @Test
    fun `switch tab uses one-based tab index`() = runTest {
        val (viewModel, _, executor) = fixture()
        val firstTabId = viewModel.activeTabId
        val secondTabId = viewModel.createTab()!!
        viewModel.selectTab(firstTabId)

        val result = executor.execute(SpringboardCommand.SwitchTab(tabIndex = 2))

        assertIs<SpringboardCommandResult.Success>(result)
        assertEquals(secondTabId, viewModel.activeTabId)
    }

    @Test
    fun `show guidance selects matching coordinate`() = runTest {
        val (viewModel, _, executor) = fixture()
        viewModel.loadConfig(guidanceSpringboardJson, "/tmp/guidance.springboard.json")

        val result = executor.execute(
            SpringboardCommand.ShowGuidance(
                environmentId = "dev",
                appId = "github",
                resourceId = "repo",
            )
        )

        assertIs<SpringboardCommandResult.Success>(result)
        assertEquals("dev", viewModel.selectedEnvironmentId)
        assertEquals("github", viewModel.selectedAppId)
        assertEquals("repo", viewModel.selectedResourceId)
    }

    private class MapContentLoader(
        private val contentsBySource: Map<String, String>,
    ) : SpringboardContentLoader {
        override suspend fun loadContent(source: String): String =
            contentsBySource[source] ?: error("Missing test content for $source")
    }

    private val guidanceSpringboardJson = """
    {
      "name": "Guidance Test",
      "environments": [
        { "id": "dev", "name": "Dev" }
      ],
      "apps": [
        { "id": "github", "name": "GitHub" }
      ],
      "resources": [
        { "id": "repo", "name": "Repository" }
      ],
      "activators": [
        { "type": "url", "appId": "github", "resourceId": "repo", "environmentId": "dev", "url": "https://github.example/repo" }
      ],
      "guidanceData": [
        {
          "environmentId": "dev",
          "appId": "github",
          "resourceId": "repo",
          "guidanceLines": ["Use this repository."]
        }
      ]
    }
    """.trimIndent()
}
