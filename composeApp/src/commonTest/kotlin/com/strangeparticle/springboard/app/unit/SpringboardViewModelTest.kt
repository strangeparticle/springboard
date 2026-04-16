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
    fun `load config parses springboard and selects first environment`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        assertNotNull(vm.springboard)
        assertTrue(vm.isConfigLoaded)
        assertEquals("preprod", vm.selectedEnvironmentId) // defaults to first env (no "all" present)
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
    }

    @Test
    fun `default environment prefers all`() {
        val jsonWithAll = """
        {
          "name": "Test",
          "environments": [
            { "id": "staging", "name": "Staging" },
            { "id": "all", "name": "All" },
            { "id": "prod", "name": "Production" }
          ],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "all", "url": "https://example.com" }
          ]
        }
        """.trimIndent()
        val vm = createViewModel()
        vm.loadConfig(jsonWithAll, "/test")
        assertEquals("all", vm.selectedEnvironmentId)
    }

    @Test
    fun `environment selection clears downstream selections`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectEnvironment("prod")
        assertEquals("prod", vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId) // cleared on env change
        assertNull(vm.selectedResourceId) // cleared on env change
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

        // In preprod, app1 has activators, app2 does not
        val states = vm.appEnabledStates
        assertEquals(true, states["app1"])
        assertEquals(false, states["app2"])
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
    fun `activate button is disabled without full selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        assertFalse(vm.isActivateEnabled)

        vm.selectApp("app1")
        assertFalse(vm.isActivateEnabled)

        vm.selectResource("res1")
        assertTrue(vm.isActivateEnabled)
    }

    @Test
    fun `environment change clears downstream selections`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectEnvironment("prod")
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
    }

    @Test
    fun `app change clears invalid resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.selectApp("app1")
        vm.selectResource("res1")

        // Switch to app2 which doesn't have res1 in preprod
        vm.selectApp("app2")
        assertNull(vm.selectedResourceId) // cleared because res1 not valid for app2 in preprod
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
}
