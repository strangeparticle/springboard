package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpringboardViewModelHideTest {

    private val springboardJson = """
    {
      "name": "Test",
      "environments": [{ "id": "env1", "name": "Env" }],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Res" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "env1", "url": "https://example.com" }
      ]
    }
    """.trimIndent()

    private fun createViewModel(
        envVars: Map<String, String> = emptyMap(),
        activationService: PlatformActivationServiceInMemoryFake = PlatformActivationServiceInMemoryFake(),
    ): Pair<SpringboardViewModel, PlatformActivationServiceInMemoryFake> {
        val settingsManager = createSettingsManagerForTest(
            target = RuntimeEnvironment.DesktopOsx,
            envVars = envVars,
        )
        val viewModel = SpringboardViewModel(
            settingsManager,
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        viewModel.loadConfig(springboardJson, "/test.json")
        return viewModel to activationService
    }

    @Test
    fun `executeActivators succeeds and hide setting on calls hide`() {
        val (viewModel, activationService) = createViewModel()

        viewModel.activateCell(Coordinate("env1", "app1", "res1"))

        assertEquals(1, activationService.hideApplicationViaPidCount)
    }

    @Test
    fun `executeActivators succeeds and hide setting off does not call hide`() {
        val (viewModel, activationService) = createViewModel(
            envVars = mapOf("SPRINGBOARD_HIDE_APP_AFTER_ACTIVATION" to "false"),
        )

        viewModel.activateCell(Coordinate("env1", "app1", "res1"))

        assertEquals(0, activationService.hideApplicationViaPidCount)
    }

    @Test
    fun `executeActivators throws and hide setting on does not call hide`() {
        val activationService = PlatformActivationServiceInMemoryFake().apply {
            openUrlsException = RuntimeException("simulated url failure")
        }
        val (viewModel, _) = createViewModel(activationService = activationService)

        assertFailsWith<RuntimeException> {
            viewModel.activateCell(Coordinate("env1", "app1", "res1"))
        }
        assertEquals(0, activationService.hideApplicationViaPidCount)
    }
}
