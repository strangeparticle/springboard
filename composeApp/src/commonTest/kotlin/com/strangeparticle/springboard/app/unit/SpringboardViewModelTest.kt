package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
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
        assertNull(vm.springboardFilteredForRuntime)
        assertNull(vm.selectedEnvironmentId)
        assertNull(vm.selectedAppId)
        assertNull(vm.selectedResourceId)
        assertFalse(vm.isConfigLoaded)
    }

    @Test
    fun `load config parses springboard and defaults environment to first`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        assertNotNull(vm.springboardFilteredForRuntime)
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
    fun `grid heading environment selection changes environment and clears app and resource`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.selectEnvironment("preprod")
        vm.selectApp("app1")
        vm.selectResource("res1")

        vm.selectEnvironmentFromGridHeading("prod")

        assertEquals("prod", vm.selectedEnvironmentId)
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

    @Test
    fun `clear multi select discards pending selection`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.toggleMultiSelect(Coordinate("preprod", "app1", "res1"))
        vm.toggleMultiSelect(Coordinate("preprod", "app1", "res2"))
        assertEquals(2, vm.multiSelectSet.size)

        vm.clearMultiSelect()
        assertTrue(vm.multiSelectSet.isEmpty())
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

        val sb = vm.springboardFilteredForRuntime
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

        val sb = vm.springboardFilteredForRuntime
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

    // Bug fix: activateColumn / activateRow used a strict env-only lookup, so a
    // column/row click in an env-specific section missed activators whose env is
    // ALL. The keynav coordinate fallback ((env, app, res) miss → (ALL, app, res))
    // now also applies to these click paths.

    @Test
    fun `activateColumn in env-specific section falls back to ALL activator when strict coordinate is empty`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        // jsonWithAllEnvsActivator has only (ALL, app1, res1); no prod-specific
        // activator for app1. Before the fix, this fired nothing.
        vm.activateColumn("prod", "app1")

        assertEquals(listOf("https://example.com/all"), activationService.openedUrls)
    }

    @Test
    fun `activateRow in env-specific section falls back to ALL activator when strict coordinate is empty`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithAllEnvsActivator, "/test.json")

        vm.activateRow("prod", "res1")

        assertEquals(listOf("https://example.com/all"), activationService.openedUrls)
    }

    private val jsonWithMixedEnvAndAllActivators = """
    {
      "name": "Mixed",
      "environments": [
        { "id": "dev", "name": "Dev" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/all" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/dev" }
      ]
    }
    """.trimIndent()

    @Test
    fun `activateColumn in env-specific section prefers env-specific activator over ALL when both exist`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithMixedEnvAndAllActivators, "/test.json")

        vm.activateColumn("dev", "app1")

        // Strict (dev, app1, res1) wins; ALL fallback is not consulted.
        assertEquals(listOf("https://example.com/dev"), activationService.openedUrls)
    }

    @Test
    fun `activateRow in env-specific section prefers env-specific activator over ALL when both exist`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithMixedEnvAndAllActivators, "/test.json")

        vm.activateRow("dev", "res1")

        assertEquals(listOf("https://example.com/dev"), activationService.openedUrls)
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

    private val jsonWithMixedAllEnvsAndEnvSpecific = """
    {
      "name": "Mixed",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "appA", "name": "A" },
        { "id": "appB", "name": "B" },
        { "id": "appC", "name": "C" }
      ],
      "resources": [
        { "id": "resX", "name": "X" },
        { "id": "resY", "name": "Y" }
      ],
      "activators": [
        { "type": "url", "appId": "appA", "resourceId": "resX", "environmentId": "ALL",  "url": "https://example.com/A/X/all" },
        { "type": "url", "appId": "appB", "resourceId": "resY", "environmentId": "dev",  "url": "https://example.com/B/Y/dev" },
        { "type": "url", "appId": "appC", "resourceId": "resX", "environmentId": "prod", "url": "https://example.com/C/X/prod" }
      ]
    }
    """.trimIndent()

    @Test
    fun `resourceEnabledStates with selected env includes resources reachable via ALL-envs activators`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("dev")
        vm.selectApp("appA")

        // appA only has an ALL-envs activator at resX. With env=dev selected the
        // resource dropdown must still enable resX since the ALL activator covers dev.
        val states = vm.resourceEnabledStates
        assertEquals(true, states["resX"], "resX should be enabled via the ALL-envs activator for appA")
        assertEquals(false, states["resY"])
    }

    @Test
    fun `appEnabledStates with selected env includes apps reachable via ALL-envs activators`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("dev")
        vm.selectResource("resX")

        // For env=dev + resX, only appA (via ALL) and (no env-specific) match.
        // Without the all-envs expansion, appA would be disabled.
        val states = vm.appEnabledStates
        assertEquals(true, states["appA"], "appA should be enabled via the ALL-envs activator at resX")
        assertEquals(false, states["appB"])
        assertEquals(false, states["appC"])
    }

    @Test
    fun `resourceEnabledStates still respects env-specific activators when no ALL match exists`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("prod")
        vm.selectApp("appB")

        // appB has an env-specific activator only at (dev, resY). With env=prod
        // and no ALL activator for appB, resY should remain disabled.
        val states = vm.resourceEnabledStates
        assertEquals(false, states["resX"])
        assertEquals(false, states["resY"])
    }

    @Test
    fun `environmentEnabledStates lights every env when selection is reachable only via ALL-envs activator`() {
        val onlyAllEnvsJson = """
        {
          "name": "Only ALL",
          "environments": [
            { "id": "dev", "name": "Dev" },
            { "id": "staging", "name": "Staging" },
            { "id": "prod", "name": "Prod" }
          ],
          "apps": [{ "id": "appOnly", "name": "Only" }],
          "resources": [{ "id": "resOnly", "name": "Only" }],
          "activators": [
            { "type": "url", "appId": "appOnly", "resourceId": "resOnly", "environmentId": "ALL", "url": "https://example.com/only" }
          ]
        }
        """.trimIndent()
        val vm = createViewModel()
        vm.loadConfig(onlyAllEnvsJson, "/test.json")
        vm.selectApp("appOnly")
        vm.selectResource("resOnly")

        // Picking any env would activate via the ALL activator (per keyNavCoordinate's
        // fallback), so every env should be enabled in the env dropdown.
        val states = vm.environmentEnabledStates
        assertEquals(true, states["dev"])
        assertEquals(true, states["staging"])
        assertEquals(true, states["prod"])
    }

    @Test
    fun `keyNavCoordinate falls back to ALL when selected env has no matching activator but ALL does`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("dev")
        vm.selectApp("appA")
        vm.selectResource("resX")

        // (dev, appA, resX) has no activator; (ALL, appA, resX) does. The
        // coordinate must resolve to the ALL one so Enter can activate it.
        assertEquals(Coordinate("ALL", "appA", "resX"), vm.keyNavCoordinate)
        assertTrue(vm.isActivateEnabled)
    }

    @Test
    fun `keyNavCoordinate prefers env-specific activator when both exist`() {
        val mixedJson = """
        {
          "name": "Both",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [{ "id": "appX", "name": "X" }],
          "resources": [{ "id": "resX", "name": "X" }],
          "activators": [
            { "type": "url", "appId": "appX", "resourceId": "resX", "environmentId": "dev", "url": "https://example.com/dev" },
            { "type": "url", "appId": "appX", "resourceId": "resX", "environmentId": "ALL", "url": "https://example.com/all" }
          ]
        }
        """.trimIndent()
        val vm = createViewModel()
        vm.loadConfig(mixedJson, "/test.json")
        vm.selectEnvironment("dev")
        vm.selectApp("appX")
        vm.selectResource("resX")

        // Strict env-specific activator wins over the ALL fallback.
        assertEquals(Coordinate("dev", "appX", "resX"), vm.keyNavCoordinate)
        assertTrue(vm.isActivateEnabled)
    }

    @Test
    fun `keyNavCoordinate keeps strict coordinate and disables activation when neither env-specific nor ALL match`() {
        val vm = createViewModel()
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("prod")
        vm.selectApp("appB")
        vm.selectResource("resY")

        // appB only has (dev, resY); no prod, no ALL. Coordinate stays strict
        // and isActivateEnabled stays false.
        assertEquals(Coordinate("prod", "appB", "resY"), vm.keyNavCoordinate)
        assertFalse(vm.isActivateEnabled)
    }

    @Test
    fun `activateCurrentSelection fires ALL-envs activator when selected env has no env-specific match`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(jsonWithMixedAllEnvsAndEnvSpecific, "/test.json")
        vm.selectEnvironment("dev")
        vm.selectApp("appA")
        vm.selectResource("resX")
        vm.activateCurrentSelection()

        assertEquals(listOf("https://example.com/A/X/all"), activationService.openedUrls)
    }

    private class RecordingLoader(
        private val contentsBySource: MutableMap<String, String>,
    ) : com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoader {
        val calls = mutableListOf<String>()
        override suspend fun loadContent(source: String): String {
            calls += source
            return contentsBySource[source] ?: throw IllegalStateException("missing source: $source")
        }
    }

    private fun jsonWithName(name: String): String = """
    {
      "name": "$name",
      "environments": [{ "id": "prod", "name": "Prod" }],
      "apps": [{ "id": "app1", "name": "App One" }],
      "resources": [{ "id": "res1", "name": "Res One" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com" }
      ]
    }
    """.trimIndent()

    @Test
    fun `reloadCurrentSource fetches latest content via loader and updates active springboard`() = kotlinx.coroutines.test.runTest {
        val contents = mutableMapOf("/foo.json" to jsonWithName("Initial"))
        val loader = RecordingLoader(contents)
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            contentLoader = loader,
        )
        vm.loadConfig(jsonWithName("Initial"), "/foo.json")

        contents["/foo.json"] = jsonWithName("Updated")
        vm.reloadCurrentSource()

        assertEquals("Updated", vm.springboardFilteredForRuntime?.name)
        assertEquals(listOf("/foo.json"), loader.calls)
    }

    @Test
    fun `reloadCurrentSource is a no-op when no springboard is loaded`() = kotlinx.coroutines.test.runTest {
        val loader = RecordingLoader(mutableMapOf())
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            contentLoader = loader,
        )

        vm.reloadCurrentSource()

        assertTrue(loader.calls.isEmpty())
    }

    @Test
    fun `reloadCurrentSource reports a toast error when the loader throws`() = kotlinx.coroutines.test.runTest {
        val loader = object : com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoader {
            override suspend fun loadContent(source: String): String =
                throw IllegalStateException("network down")
        }
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            contentLoader = loader,
        )
        vm.loadConfig(jsonWithName("Initial"), "/foo.json")

        vm.reloadCurrentSource()

        assertEquals("Initial", vm.springboardFilteredForRuntime?.name)
        val errorToasts = vm.activeTabToast.activeToasts.filter {
            it.severity == com.strangeparticle.springboard.app.ui.toast.ToastSeverity.ERROR
        }
        assertTrue(errorToasts.any { it.message.contains("network down") })
    }

    @Test
    fun `reloadCurrentSource reports a toast error when content loader is missing`() = kotlinx.coroutines.test.runTest {
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
        )
        vm.loadConfig(jsonWithName("Initial"), "/foo.json")

        vm.reloadCurrentSource()

        assertEquals("Initial", vm.springboardFilteredForRuntime?.name)
        val errorToasts = vm.activeTabToast.activeToasts.filter {
            it.severity == com.strangeparticle.springboard.app.ui.toast.ToastSeverity.ERROR
        }
        assertTrue(errorToasts.any { it.message.contains("SpringboardContentLoader") })
    }

    @Test
    fun `loadConfigFromSource replacing current tab clears stale toasts before replacement toasts`() = kotlinx.coroutines.test.runTest {
        val loader = RecordingLoader(mutableMapOf("/replacement.json" to TestFixtureJson.COMMAND_ACTIVATOR))
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            contentLoader = loader,
        )
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/initial.json")
        vm.activeTabToast.error("stale notification")

        val result = vm.loadConfigFromSource("/replacement.json", inNewTab = false)

        assertEquals(SpringboardViewModel.LoadResult.Success(vm.activeTabId), result)
        val messages = vm.activeTabToast.activeToasts.map { it.message }
        assertFalse(messages.any { it.contains("stale notification") })
        assertFalse(messages.any { it.contains("URL Only Springboard") })
        assertTrue(messages.any { it.contains("execute CLI commands") })
        assertTrue(messages.any { it == "Springboard loaded: Command Springboard" })
    }

    @Test
    fun `loadConfigFromSource replacing current tab keeps stale toasts when replacement fails`() = kotlinx.coroutines.test.runTest {
        val loader = RecordingLoader(mutableMapOf())
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(),
            PersistenceServiceInMemoryFake(),
            contentLoader = loader,
        )
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/initial.json")
        vm.activeTabToast.error("stale notification")

        val result = vm.loadConfigFromSource("/missing.json", inNewTab = false)

        assertTrue(result is SpringboardViewModel.LoadResult.Failure)
        val messages = vm.activeTabToast.activeToasts.map { it.message }
        assertTrue(messages.any { it.contains("stale notification") })
        assertTrue(messages.any { it.contains("URL Only Springboard") })
    }

    @Test
    fun `wasm stores filtered springboard for UI and unfiltered springboard for data integrity`() {
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(target = RuntimeEnvironment.WASM),
            PersistenceServiceInMemoryFake(),
        )

        vm.loadConfig(TestFixtureJson.COMMAND_ACTIVATOR, "/foo.json")

        val coordinate = Coordinate("dev", "app1", "res1")
        assertNull(vm.springboardFilteredForRuntime?.indexes?.activatorByCoordinate?.get(coordinate))
        assertNotNull(vm.springboardUnfiltered?.indexes?.activatorByCoordinate?.get(coordinate))
    }

    @Test
    fun `wasm keynav ignores hidden strict command activator and falls back to all-envs url`() {
        val activationService = com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake()
        val vm = SpringboardViewModel(
            createSettingsManagerForTest(target = RuntimeEnvironment.WASM),
            PersistenceServiceInMemoryFake(),
            activationService,
        )
        vm.loadConfig(TestFixtureJson.COMMAND_STRICT_WITH_ALL_ENVS_URL_FALLBACK, "/foo.json")

        vm.selectEnvironment("prod")
        vm.selectApp("app1")
        vm.selectResource("res1")

        assertEquals(Coordinate("ALL", "app1", "res1"), vm.keyNavCoordinate)

        vm.activateCurrentSelection()

        assertEquals(listOf("https://example.com/all/app1/res1"), activationService.openedUrls)
        assertTrue(activationService.executedCommands.isEmpty())
    }

    // ---- isDirty + mark/clear (Task 3) ----

    @Test
    fun `markActiveTabDirty sets isDirty true on the active tab`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")

        vm.markActiveTabDirty()

        assertEquals(true, vm.activeTab?.isDirty)
    }

    @Test
    fun `clearActiveTabDirty sets isDirty false on the active tab`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test.json")
        vm.markActiveTabDirty()

        vm.clearActiveTabDirty()

        assertEquals(false, vm.activeTab?.isDirty)
    }

    @Test
    fun `loadConfig clears any pre-existing dirty flag on the active tab`() {
        val vm = createViewModel()
        vm.loadConfig(validJson, "/test1.json")
        vm.markActiveTabDirty()
        assertEquals(true, vm.activeTab?.isDirty)

        // Loading a new config should reset dirty (just-loaded matches disk).
        vm.loadConfig(validJson, "/test2.json")

        assertEquals(false, vm.activeTab?.isDirty)
    }

    @Test
    fun `freshly-created empty tab is not dirty`() {
        val vm = createViewModel()

        assertEquals(false, vm.activeTab?.isDirty)
    }
}
