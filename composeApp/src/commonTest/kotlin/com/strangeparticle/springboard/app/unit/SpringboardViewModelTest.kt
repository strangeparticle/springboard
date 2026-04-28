package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.*
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake

class SpringboardViewModelTest {

    private val validJson = """
    {
      "name": "Test Springboard",
      "environments": [
        { "id": "preprod", "name": "Preprod" },
        { "id": "prod", "name": "Production" }
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "preprod", "url": "https://example.com/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/dash" }
      ]
    }
    """.trimIndent()

    private fun createViewModel() = SpringboardViewModel(createSettingsManagerForTest(), PersistenceServiceInMemoryFake())

    @Test
    fun `initial state has no springboard loaded`() {
        val vm = createViewModel()
        assertNull(vm.springboard)
        assertNull(vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
        assertFalse(vm.isConfigLoaded)
    }

    @Test
    fun `load config parses springboard and defaults environment to first`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        assertNotNull(vm.springboard)
        assertTrue(vm.isConfigLoaded)
        assertEquals("preprod", vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
    }

    @Test
    fun `default environment is first in list`() {
        val jsonWithMultipleEnvironments = """
        {
          "name": "Test",
          "environments": [
            { "id": "staging", "name": "Staging" },
            { "id": "preprod", "name": "Preprod" },
            { "id": "prod", "name": "Production" }
          ],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "staging", "url": "https://example.com" }
          ]
        }
        """.trimIndent()
        val vm = createViewModel()
        vm.loadConfig(jsonWithMultipleEnvironments, "/test")
        assertEquals("staging", vm.selectedEnvironmentId)
    }

    @Test
    fun `environment selection does not clear other selections`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        vm.selectResource("res1")
        vm.selectEnvironment("prod")

        assertEquals("prod", vm.selectedEnvironmentId)
        assertEquals("app1", vm.selectedAppId)
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `environment selection null clears only environment selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectEnvironment("preprod")
        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectEnvironment(null)

        assertNull(vm.selectedEnvironmentId)
        assertEquals("app1", vm.selectedAppId)
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `app selection null clears only app selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectEnvironment("preprod")
        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectApp(null)

        assertEquals("preprod", vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId)
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `app selection updates selected app`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        assertEquals("app1", vm.selectedAppId)
    }

    @Test
    fun `resource selection updates selected resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        vm.selectResource("res1")
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `app enabled states reflect activator availability`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        // With env=null (explicit None), app enabled states are computed across all environments.
        vm.selectEnvironment(null)
        val states = vm.appEnabledStates
        assertEquals(true, states["app1"])
        assertEquals(true, states["app2"])
    }

    @Test
    fun `resource enabled states reflect activator availability`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        val states = vm.resourceEnabledStates
        assertEquals(true, states["res1"])
        assertEquals(true, states["res2"])
    }

    @Test
    fun `resource enabled states are available before app selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        // In preprod, app1 exposes both resources, so both should be selectable even
        // before app is chosen.
        val states = vm.resourceEnabledStates
        assertEquals(true, states["res1"])
        assertEquals(true, states["res2"])
    }

    @Test
    fun `app enabled states are filtered by selected resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.selectResource("res2")

        val states = vm.appEnabledStates
        assertEquals(true, states["app1"])
        assertEquals(false, states["app2"])
    }

    @Test
    fun `environment enabled states are filtered by selected app and resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.selectApp("app2")
        vm.selectResource("res1")

        val states = vm.environmentEnabledStates
        assertEquals(false, states["preprod"])
        assertEquals(true, states["prod"])
    }

    @Test
    fun `activate button is disabled without full selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.selectEnvironment(null)

        assertFalse(vm.isActivateEnabled)

        vm.selectApp("app1")
        assertFalse(vm.isActivateEnabled)

        vm.selectResource("res1")
        assertFalse(vm.isActivateEnabled)

        vm.selectEnvironment("preprod")
        assertTrue(vm.isActivateEnabled)
    }

    @Test
    fun `environment change retains app and resource even when coordinate becomes invalid`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectEnvironment("prod")
        assertEquals("app1", vm.selectedAppId)
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `environment change retains app and resource when coordinate remains valid`() {
        val jsonWithSharedCoordinate = """
        {
          "name": "Shared Coordinate Springboard",
          "environments": [
            { "id": "preprod", "name": "Preprod" },
            { "id": "prod", "name": "Production" }
          ],
          "apps": [
            { "id": "app1", "name": "App One" }
          ],
          "resources": [
            { "id": "res1", "name": "Dashboard" }
          ],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/dash" },
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/dash" }
          ]
        }
        """.trimIndent()

        val vm = createViewModel()
        vm.loadConfig(jsonWithSharedCoordinate, "/test.json")
        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectEnvironment("prod")

        assertEquals("app1", vm.selectedAppId)
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `app change does not clear resource even when coordinate becomes invalid`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.selectEnvironment("preprod")

        vm.selectApp("app1")
        vm.selectResource("res1")

        // Switch to app2 which doesn't have res1 in preprod; resource stays selected so
        // the user can see it as disabled instead of silently losing it.
        vm.selectApp("app2")
        assertEquals("res1", vm.selectedResourceId)
    }

    @Test
    fun `resetKeyNavSelections restores environment default and clears app and resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectEnvironment("prod")
        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.resetKeyNavSelections()

        assertEquals("preprod", vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
    }

    @Test
    fun `get activator for cell returns correct activator`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        val activator = vm.getActivatorForCell(Coordinate("preprod", "app1", "res1"))
        assertNotNull(activator)

        val missing = vm.getActivatorForCell(Coordinate("preprod", "app2", "res1"))
        assertNull(missing)
    }

    @Test
    fun `multi select toggles coordinates`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        val coord1 = Coordinate("preprod", "app1", "res1")
        val coord2 = Coordinate("preprod", "app1", "res2")

        vm.toggleMultiSelect(coord1)
        assertEquals(setOf(coord1), vm.multiSelectSet)

        vm.toggleMultiSelect(coord2)
        assertEquals(setOf(coord1, coord2), vm.multiSelectSet)

        // Toggle off
        vm.toggleMultiSelect(coord1)
        assertEquals(setOf(coord2), vm.multiSelectSet)
    }

    // --- Guidance data through ViewModel load ---

    private val jsonWithGuidance = """
    {
      "name": "Guided Springboard",
      "environments": [
        { "id": "preprod", "name": "Preprod" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/dash" }
      ],
      "guidanceData": [
        {
          "environmentId": "preprod",
          "appId": "app1",
          "resourceId": "res1",
          "guidanceLines": ["Log in first.", "Select the team workspace."]
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `guidance data is available after load`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithGuidance, "/test.json")

        val sb = vm.springboard
        assertNotNull(sb)
        assertEquals(1, sb.guidanceData.size)

        val coord = Coordinate("preprod", "app1", "res1")
        val guidance = sb.indexes.guidanceByCoordinate[coord]
        assertNotNull(guidance)
        assertEquals(listOf("Log in first.", "Select the team workspace."), guidance.guidanceLines)
    }

    @Test
    fun `no guidance data still loads successfully`() {
        // The original validJson has no guidanceData
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        val sb = vm.springboard
        assertNotNull(sb)
        assertTrue(sb.guidanceData.isEmpty())
        assertTrue(sb.indexes.guidanceByCoordinate.isEmpty())
    }

    private val jsonWithAllEnvsActivator = """
    {
      "name": "All-envs Activator",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/all" },
        { "type": "url", "appId": "app2", "resourceId": "res2", "environmentId": "prod", "url": "https://example.com/prod" }
      ]
    }
    """.trimIndent()

    @Test
    fun `keyNavCoordinate falls back to ALL when env unselected and app and resource are set`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.selectEnvironment(null)
        vm.selectApp("app1")
        vm.selectResource("res1")

        assertEquals(Coordinate("ALL", "app1", "res1"), vm.keyNavCoordinate)
    }

    @Test
    fun `isActivateEnabled is true for ALL coordinate when env unselected`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.selectEnvironment(null)
        vm.selectApp("app1")
        vm.selectResource("res1")

        assertTrue(vm.isActivateEnabled)
    }

    @Test
    fun `activateCurrentSelection executes ALL activator with no env selected`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.selectEnvironment(null)
        vm.selectApp("app1")
        vm.selectResource("res1")
        vm.activateCurrentSelection()

        assertEquals(listOf("https://example.com/all"), activationService.openedUrls)
    }

    @Test
    fun `activateColumn for ALL env executes only ALL activators in that column`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.activateColumn("ALL", "app1")

        // Only the ALL activator at (ALL, app1, res1) should have run.
        assertEquals(listOf("https://example.com/all"), activationService.openedUrls)
    }

    @Test
    fun `activateRow for ALL env executes only ALL activators in that row`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.activateRow("ALL", "res1")

        assertEquals(listOf("https://example.com/all"), activationService.openedUrls)
    }

    @Test
    fun `apps property returns apps in visual column order when groups are declared`() {
        val groupedJson = """
        {
          "name": "Grouped",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [
            { "id": "a1", "name": "A1", "appGroupId": "g1" },
            { "id": "a2", "name": "A2", "appGroupId": "g2" },
            { "id": "a3", "name": "A3", "appGroupId": "g1" },
            { "id": "a4", "name": "A4" }
          ],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "dev", "url": "https://example.com/a1" },
            { "type": "url", "appId": "a2", "resourceId": "r1", "environmentId": "dev", "url": "https://example.com/a2" },
            { "type": "url", "appId": "a3", "resourceId": "r1", "environmentId": "dev", "url": "https://example.com/a3" },
            { "type": "url", "appId": "a4", "resourceId": "r1", "environmentId": "dev", "url": "https://example.com/a4" }
          ],
          "appGroups": [
            { "id": "g1", "description": "Group 1" },
            { "id": "g2", "description": "Group 2" }
          ]
        }
        """.trimIndent()
        val vm = createViewModel()
        vm.loadConfig(groupedJson, "/test.json")

        // Visual layout is [a1, a3, sep, a2, sep, a4]; the dropdown's app order
        // should match the visible column order, with separators filtered out.
        assertEquals(listOf("a1", "a3", "a2", "a4"), vm.apps.map { it.id })
    }

    @Test
    fun `apps property preserves declaration order when no groups declared`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        assertEquals(listOf("app1", "app2"), vm.apps.map { it.id })
    }
}
