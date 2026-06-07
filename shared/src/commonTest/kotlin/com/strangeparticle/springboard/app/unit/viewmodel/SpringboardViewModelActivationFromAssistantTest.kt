package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.AssistantActivationOutcome
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SpringboardViewModelActivationFromAssistantTest {

    private val springboardJson = """
    {
      "name": "Test",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Prod" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" },
        { "id": "app2", "name": "App Two" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://dev.example.com/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "dev", "url": "https://dev.example.com/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "ALL", "url": "https://all.example.com/app2-dash" },
        { "type": "cmd", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "commandTemplate": "echo prod-dash" }
      ]
    }
    """.trimIndent()

    private val otherSpringboardJson = """
    {
      "name": "Other",
      "environments": [{ "id": "dev", "name": "Dev" }],
      "apps": [{ "id": "appX", "name": "App X" }],
      "resources": [{ "id": "resX", "name": "Res X" }],
      "activators": [
        { "type": "url", "appId": "appX", "resourceId": "resX", "environmentId": "dev", "url": "https://other.example.com/x" }
      ]
    }
    """.trimIndent()

    private data class Fixture(
        val viewModel: SpringboardViewModel,
        val activationService: PlatformActivationServiceInMemoryFake,
    )

    private fun fixture(): Fixture {
        val activationService = PlatformActivationServiceInMemoryFake()
        val viewModel = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        return Fixture(viewModel, activationService)
    }

    @Test
    fun `activateCoordinateFromAssistant returns MissingTab for unknown tab id`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = "no-such-tab",
            coordinate = Coordinate("dev", "app1", "res1"),
        )

        assertEquals(AssistantActivationOutcome.MissingTab, outcome)
        assertTrue(activationService.openedUrls.isEmpty())
    }

    @Test
    fun `activateCoordinateFromAssistant returns TabEmpty when tab has no springboard loaded`() {
        val (vm, activationService) = fixture()
        val emptyTabId = vm.activeTabId

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = emptyTabId,
            coordinate = Coordinate("dev", "app1", "res1"),
        )

        assertEquals(AssistantActivationOutcome.TabEmpty, outcome)
        assertTrue(activationService.openedUrls.isEmpty())
    }

    @Test
    fun `activateCoordinateFromAssistant returns NoActivatorsResolved when coordinate has no activator`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = vm.activeTabId,
            coordinate = Coordinate("dev", "app2", "res2"),
        )

        assertEquals(AssistantActivationOutcome.NoActivatorsResolved, outcome)
        assertTrue(activationService.openedUrls.isEmpty())
    }

    @Test
    fun `activateCoordinateFromAssistant opens URL for active tab`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = vm.activeTabId,
            coordinate = Coordinate("dev", "app1", "res1"),
        )

        assertEquals(AssistantActivationOutcome.Success(1), outcome)
        assertEquals(listOf("https://dev.example.com/dash"), activationService.openedUrls)
    }

    @Test
    fun `activateCoordinateFromAssistant opens URL for non-active tab and leaves activeTabId unchanged`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")
        val firstTabId = vm.activeTabId
        val secondTabId = vm.createTab()!!
        vm.loadConfig(otherSpringboardJson, "/b.json")
        vm.selectTab(firstTabId)

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = secondTabId,
            coordinate = Coordinate("dev", "appX", "resX"),
        )

        assertEquals(AssistantActivationOutcome.Success(1), outcome)
        assertEquals(listOf("https://other.example.com/x"), activationService.openedUrls)
        assertEquals(firstTabId, vm.activeTabId)
    }

    @Test
    fun `activateCoordinateFromAssistant runs command for command activator`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinateFromAssistant(
            tabId = vm.activeTabId,
            coordinate = Coordinate("prod", "app1", "res1"),
        )

        assertEquals(AssistantActivationOutcome.Success(1), outcome)
        assertTrue(activationService.openedUrls.isEmpty())
        assertEquals(listOf("echo prod-dash"), activationService.executedCommands)
    }

    @Test
    fun `activateRowFromAssistant uses all-envs fallback in env-specific section`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        // res1 row in "prod": app1 has a prod command, app2 has ALL fallback.
        val outcome = vm.activateRowFromAssistant(
            tabId = vm.activeTabId,
            environmentId = "prod",
            resourceId = "res1",
        )

        assertEquals(AssistantActivationOutcome.Success(2), outcome)
        assertEquals(listOf("https://all.example.com/app2-dash"), activationService.openedUrls)
        assertEquals(listOf("echo prod-dash"), activationService.executedCommands)
    }

    @Test
    fun `activateColumnFromAssistant uses all-envs fallback in env-specific section`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        // app2 column in "prod": only an ALL-env URL exists, should be picked up.
        val outcome = vm.activateColumnFromAssistant(
            tabId = vm.activeTabId,
            environmentId = "prod",
            appId = "app2",
        )

        assertEquals(AssistantActivationOutcome.Success(1), outcome)
        assertEquals(listOf("https://all.example.com/app2-dash"), activationService.openedUrls)
    }

    @Test
    fun `activateCoordinatesFromAssistant batches multi-coordinate activation`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinatesFromAssistant(
            tabId = vm.activeTabId,
            coordinates = listOf(
                Coordinate("dev", "app1", "res1"),
                Coordinate("dev", "app1", "res2"),
            ),
        )

        assertEquals(AssistantActivationOutcome.Success(2), outcome)
        assertEquals(
            listOf("https://dev.example.com/dash", "https://dev.example.com/logs"),
            activationService.openedUrls,
        )
    }

    @Test
    fun `activateCoordinatesFromAssistant skips unmatched coordinates but succeeds when at least one matches`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        val outcome = vm.activateCoordinatesFromAssistant(
            tabId = vm.activeTabId,
            coordinates = listOf(
                Coordinate("dev", "app1", "res1"),
                Coordinate("dev", "app2", "res2"), // no activator
            ),
        )

        assertEquals(AssistantActivationOutcome.Success(1), outcome)
        assertEquals(listOf("https://dev.example.com/dash"), activationService.openedUrls)
    }

    @Test
    fun `activateRowFromAssistant returns NoActivatorsResolved when row has no activators`() {
        val (vm, activationService) = fixture()
        vm.loadConfig(springboardJson, "/a.json")

        // res2 row in "prod" — no prod activator and no ALL activator.
        val outcome = vm.activateRowFromAssistant(
            tabId = vm.activeTabId,
            environmentId = "prod",
            resourceId = "res2",
        )

        assertEquals(AssistantActivationOutcome.NoActivatorsResolved, outcome)
        assertTrue(activationService.openedUrls.isEmpty())
        assertTrue(activationService.executedCommands.isEmpty())
    }
}
